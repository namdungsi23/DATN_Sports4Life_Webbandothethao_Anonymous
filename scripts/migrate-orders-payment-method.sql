-- Thêm cột phương thức thanh toán cho đơn hàng (SePay / COD / ...)
IF COL_LENGTH('dbo.Orders', 'PaymentMethod') IS NULL
BEGIN
    ALTER TABLE dbo.Orders ADD PaymentMethod VARCHAR(50) NULL;
END
GO
