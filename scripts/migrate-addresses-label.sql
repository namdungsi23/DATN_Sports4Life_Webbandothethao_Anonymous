-- Migration: cập nhật bảng Addresses (chạy trên DB đã tồn tại)
-- USE [Sports4Life]
-- GO

IF COL_LENGTH('dbo.Addresses', 'Label') IS NULL
BEGIN
    ALTER TABLE [dbo].[Addresses] ADD [Label] [nvarchar](50) NULL;
END
GO

IF COL_LENGTH('dbo.Addresses', 'ReceiverName') IS NOT NULL
BEGIN
    ALTER TABLE [dbo].[Addresses] DROP COLUMN [ReceiverName];
END
GO

IF COL_LENGTH('dbo.Addresses', 'ReceiverPhone') IS NOT NULL
BEGIN
    ALTER TABLE [dbo].[Addresses] DROP COLUMN [ReceiverPhone];
END
GO

UPDATE [dbo].[Addresses]
SET [Label] = N'Địa chỉ mặc định'
WHERE [IsDefault] = 1 AND ([Label] IS NULL OR [Label] = '');
GO
