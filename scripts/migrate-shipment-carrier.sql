-- Carriers & Shipments for order shipping management
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'Carriers')
BEGIN
    CREATE TABLE Carriers (
        Id INT IDENTITY(1,1) PRIMARY KEY,
        Name NVARCHAR(100) NOT NULL,
        Code VARCHAR(50) NULL,
        Active BIT NOT NULL DEFAULT 1
    );

    INSERT INTO Carriers (Name, Code, Active) VALUES
        (N'Giao Hàng Nhanh', 'GHN', 1),
        (N'Giao Hàng Tiết Kiệm', 'GHTK', 1),
        (N'Viettel Post', 'VTP', 1),
        (N'J&T Express', 'JNT', 1),
        (N'VNPost', 'VNPOST', 1);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'Shipments')
BEGIN
    CREATE TABLE Shipments (
        Id INT IDENTITY(1,1) PRIMARY KEY,
        OrderId INT NOT NULL UNIQUE,
        CarrierId INT NULL,
        TrackingNumber VARCHAR(100) NULL,
        ShippingStatus VARCHAR(50) NOT NULL DEFAULT 'PENDING',
        ShippingFee DECIMAL(12,2) NOT NULL DEFAULT 0,
        Notes NVARCHAR(500) NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT GETDATE(),
        UpdatedAt DATETIME2 NULL,
        CONSTRAINT FK_Shipments_Orders FOREIGN KEY (OrderId) REFERENCES Orders(Id),
        CONSTRAINT FK_Shipments_Carriers FOREIGN KEY (CarrierId) REFERENCES Carriers(Id)
    );
END
GO
