-- ============================================================
-- Sports4Life — sync schema theo entity/app hiện tại
-- Chạy trên DB Sports4Life (SQL Server)
-- An toàn: chỉ ADD nếu thiếu, không drop dữ liệu
-- ============================================================
USE [Sports4Life];
GO

SET NOCOUNT ON;

-- ---------- CartItems: ProductId -> ProductVariantId ----------
IF COL_LENGTH('dbo.CartItems', 'ProductId') IS NOT NULL
   AND COL_LENGTH('dbo.CartItems', 'ProductVariantId') IS NULL
BEGIN
    ALTER TABLE dbo.CartItems ADD ProductVariantId INT NULL;
END
GO

IF COL_LENGTH('dbo.CartItems', 'ProductId') IS NOT NULL
   AND COL_LENGTH('dbo.CartItems', 'ProductVariantId') IS NOT NULL
BEGIN
    -- Map tạm: lấy biến thể mặc định (hoặc biến thể đầu) của ProductId cũ
    UPDATE ci
    SET ci.ProductVariantId = COALESCE(
        (SELECT TOP 1 pv.Id FROM dbo.ProductVariants pv
         WHERE pv.ProductId = ci.ProductId AND pv.IsDefault = 1
         ORDER BY pv.Id),
        (SELECT TOP 1 pv.Id FROM dbo.ProductVariants pv
         WHERE pv.ProductId = ci.ProductId
         ORDER BY pv.Id)
    )
    FROM dbo.CartItems ci
    WHERE ci.ProductVariantId IS NULL AND ci.ProductId IS NOT NULL;

    DECLARE @fk NVARCHAR(200);
    SELECT @fk = fk.name
    FROM sys.foreign_keys fk
    INNER JOIN sys.foreign_key_columns fkc ON fkc.constraint_object_id = fk.object_id
    INNER JOIN sys.columns c ON c.object_id = fkc.parent_object_id AND c.column_id = fkc.parent_column_id
    WHERE fk.parent_object_id = OBJECT_ID('dbo.CartItems') AND c.name = 'ProductId';

    IF @fk IS NOT NULL
        EXEC('ALTER TABLE dbo.CartItems DROP CONSTRAINT [' + @fk + ']');

    ALTER TABLE dbo.CartItems DROP COLUMN ProductId;
END
GO

IF COL_LENGTH('dbo.CartItems', 'ProductVariantId') IS NOT NULL
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_keys
        WHERE parent_object_id = OBJECT_ID('dbo.CartItems')
          AND name = 'FK_CartItems_ProductVariants'
    )
    BEGIN
        ALTER TABLE dbo.CartItems
        ADD CONSTRAINT FK_CartItems_ProductVariants
            FOREIGN KEY (ProductVariantId) REFERENCES dbo.ProductVariants(Id);
    END
END
GO

-- ---------- Addresses.Label ----------
IF COL_LENGTH('dbo.Addresses', 'Label') IS NULL
BEGIN
    ALTER TABLE dbo.Addresses ADD Label NVARCHAR(100) NULL;
END
GO

-- ---------- Carriers ----------
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'Carriers')
BEGIN
    CREATE TABLE dbo.Carriers (
        Id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        Name NVARCHAR(100) NOT NULL,
        Code VARCHAR(50) NULL,
        Active BIT NOT NULL CONSTRAINT DF_Carriers_Active DEFAULT (1)
    );

    INSERT INTO dbo.Carriers (Name, Code, Active) VALUES
        (N'Giao Hàng Nhanh', 'GHN', 1),
        (N'Giao Hàng Tiết Kiệm', 'GHTK', 1),
        (N'Viettel Post', 'VTP', 1),
        (N'J&T Express', 'JNT', 1),
        (N'VNPost', 'VNPOST', 1);
END
GO

-- ---------- Shipments ----------
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'Shipments')
BEGIN
    CREATE TABLE dbo.Shipments (
        Id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        OrderId INT NOT NULL,
        CarrierId INT NULL,
        TrackingNumber VARCHAR(100) NULL,
        ShippingStatus VARCHAR(50) NOT NULL CONSTRAINT DF_Shipments_Status DEFAULT ('PENDING'),
        ShippingFee DECIMAL(12,2) NOT NULL CONSTRAINT DF_Shipments_Fee DEFAULT (0),
        Notes NVARCHAR(500) NULL,
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Shipments_CreatedAt DEFAULT (SYSUTCDATETIME()),
        UpdatedAt DATETIME2 NULL,
        CONSTRAINT UQ_Shipments_OrderId UNIQUE (OrderId),
        CONSTRAINT FK_Shipments_Orders FOREIGN KEY (OrderId) REFERENCES dbo.Orders(Id),
        CONSTRAINT FK_Shipments_Carriers FOREIGN KEY (CarrierId) REFERENCES dbo.Carriers(Id)
    );
END
GO

-- ---------- OrderAddresses ----------
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'OrderAddresses')
BEGIN
    CREATE TABLE dbo.OrderAddresses (
        Id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        OrderId INT NOT NULL,
        ReceiverName NVARCHAR(100) NOT NULL,
        ReceiverPhone VARCHAR(20) NOT NULL,
        Province NVARCHAR(100) NOT NULL,
        Ward NVARCHAR(100) NOT NULL,
        AddressDetail NVARCHAR(255) NOT NULL,
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_OrderAddresses_CreatedAt DEFAULT (SYSUTCDATETIME()),
        UpdatedAt DATETIME2 NULL,
        CONSTRAINT FK_OrderAddresses_Orders FOREIGN KEY (OrderId) REFERENCES dbo.Orders(Id)
    );
END
GO

-- ---------- Notifications (cho entity Notification) ----------
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'Notifications')
BEGIN
    CREATE TABLE dbo.Notifications (
        Id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        UserId INT NOT NULL,
        Title NVARCHAR(200) NULL,
        Message NVARCHAR(MAX) NULL,
        Link NVARCHAR(255) NULL,
        IsRead BIT NOT NULL CONSTRAINT DF_Notifications_IsRead DEFAULT (0),
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Notifications_CreatedAt DEFAULT (SYSUTCDATETIME()),
        CONSTRAINT FK_Notifications_Users FOREIGN KEY (UserId) REFERENCES dbo.Users(AccountId)
    );
    CREATE INDEX IX_Notifications_UserId_CreatedAt ON dbo.Notifications(UserId, CreatedAt DESC);
END
GO

IF COL_LENGTH('dbo.Notifications', 'Link') IS NULL
BEGIN
    ALTER TABLE dbo.Notifications ADD Link NVARCHAR(255) NULL;
END
GO

-- ---------- Wishlists (yêu thích) ----------
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'Wishlists')
BEGIN
    CREATE TABLE dbo.Wishlists (
        Id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        UserId INT NOT NULL,
        ProductId INT NOT NULL,
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Wishlists_CreatedAt DEFAULT (SYSUTCDATETIME()),
        CONSTRAINT FK_Wishlists_Users FOREIGN KEY (UserId) REFERENCES dbo.Users(AccountId),
        CONSTRAINT FK_Wishlists_Products FOREIGN KEY (ProductId) REFERENCES dbo.Products(Id),
        CONSTRAINT UQ_Wishlists_User_Product UNIQUE (UserId, ProductId)
    );
    CREATE INDEX IX_Wishlists_UserId_CreatedAt ON dbo.Wishlists(UserId, CreatedAt DESC);
END
GO

PRINT N'Sync schema Sports4Life hoàn tất.';
GO

-- ---------- Comments (binh luan + rating) ----------
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'Comments')
BEGIN
    CREATE TABLE dbo.Comments (
        Id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        ProductId INT NOT NULL,
        UserId INT NOT NULL,
        Content NVARCHAR(1000) NOT NULL,
        Rating TINYINT NOT NULL,
        CreatedAt DATETIME NULL CONSTRAINT DF_Comments_CreatedAt DEFAULT (GETDATE()),
        UpdatedAt DATETIME NULL,
        Status BIT NULL CONSTRAINT DF_Comments_Status DEFAULT (1),
        CONSTRAINT FK_Comments_Products FOREIGN KEY (ProductId) REFERENCES dbo.Products(Id),
        CONSTRAINT FK_Comments_Users FOREIGN KEY (UserId) REFERENCES dbo.Users(AccountId),
        CONSTRAINT CK_Comments_Rating CHECK (Rating BETWEEN 1 AND 5)
    );
    CREATE INDEX IX_Comments_ProductId_CreatedAt ON dbo.Comments(ProductId, CreatedAt DESC);
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes WHERE name = 'UQ_Comments_User_Product' AND object_id = OBJECT_ID('dbo.Comments')
)
BEGIN
    CREATE UNIQUE INDEX UQ_Comments_User_Product ON dbo.Comments(UserId, ProductId);
END
GO

