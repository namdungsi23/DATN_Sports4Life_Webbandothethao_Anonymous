-- Fix Vouchers column types to prevent numeric overflow on insert
-- Run on SQL Server if creating voucher fails with "Arithmetic overflow"

IF COL_LENGTH('dbo.Vouchers', 'DiscountPercent') IS NOT NULL
BEGIN
    ALTER TABLE dbo.Vouchers ALTER COLUMN DiscountPercent int NULL;
END
GO

IF COL_LENGTH('dbo.Vouchers', 'DiscountAmount') IS NOT NULL
BEGIN
    ALTER TABLE dbo.Vouchers ALTER COLUMN DiscountAmount decimal(10, 2) NULL;
END
GO

IF COL_LENGTH('dbo.Vouchers', 'MinOrderValue') IS NOT NULL
BEGIN
    ALTER TABLE dbo.Vouchers ALTER COLUMN MinOrderValue decimal(10, 2) NULL;
END
GO

IF COL_LENGTH('dbo.Vouchers', 'MaxDiscount') IS NOT NULL
BEGIN
    ALTER TABLE dbo.Vouchers ALTER COLUMN MaxDiscount decimal(10, 2) NULL;
END
GO

IF COL_LENGTH('dbo.Vouchers', 'IsActive') IS NOT NULL
BEGIN
    ALTER TABLE dbo.Vouchers ALTER COLUMN IsActive tinyint NULL;
END
GO
