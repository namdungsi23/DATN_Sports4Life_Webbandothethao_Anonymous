/*
  Balo / Túi / Tất / phụ kiện: chỉ giữ size Free (bỏ S-M-L-XL nhầm category)
*/
SET XACT_ABORT ON;
SET NOCOUNT ON;
BEGIN TRANSACTION;

IF OBJECT_ID('tempdb..#Acc') IS NOT NULL DROP TABLE #Acc;
SELECT p.Id AS ProductId
INTO #Acc
FROM Products p
WHERE p.Name LIKE N'Balo%'
   OR p.Name LIKE N'Túi%'
   OR p.Name LIKE N'Tui%'
   OR p.Name LIKE N'Tất%'
   OR p.Name LIKE N'Tat%'
   OR p.Name LIKE N'Băng%'
   OR p.Name LIKE N'Bang%'
   OR p.Name LIKE N'Găng%'
   OR p.Name LIKE N'Gang%'
   OR p.Name LIKE N'Dây%'
   OR p.Name LIKE N'Day%'
   OR p.Name LIKE N'Khăn%'
   OR p.Name LIKE N'Khan%'
   OR p.Name LIKE N'Bình%'
   OR p.Name LIKE N'Binh%'
   OR p.Name LIKE N'Bó gối%'
   OR p.Name LIKE N'Bo goi%'
   OR p.Name LIKE N'Đai%'
   OR p.Name LIKE N'Dai%'
   OR p.Name LIKE N'BÓNG%'
   OR p.Name LIKE N'BONG%';

-- Chuẩn hóa màu
UPDATE v SET Color = N'Đen'
FROM ProductVariants v
INNER JOIN #Acc a ON a.ProductId = v.ProductId
WHERE v.Color IN (N'Den', N'Đen') OR (LEN(ISNULL(v.Color,N''))=3 AND LOWER(SUBSTRING(v.Color,2,2))=N'en');

UPDATE v SET Color = N'Trắng'
FROM ProductVariants v INNER JOIN #Acc a ON a.ProductId = v.ProductId
WHERE v.Color IN (N'Trang', N'Trắng') OR (v.Color LIKE N'Tr%ng' AND LEN(v.Color) BETWEEN 5 AND 6);

UPDATE v SET Color = N'Đỏ'
FROM ProductVariants v INNER JOIN #Acc a ON a.ProductId = v.ProductId
WHERE v.Color IN (N'Do', N'Đỏ') OR (LEN(ISNULL(v.Color,N''))<=2 AND (v.Color LIKE N'D%' OR v.Color LIKE N'Đ%'));

UPDATE v SET Color = N'Xám'
FROM ProductVariants v INNER JOIN #Acc a ON a.ProductId = v.ProductId
WHERE v.Color IN (N'Xam', N'Xám') OR (v.Color LIKE N'X_m' AND LEN(v.Color)=3);

-- Giữ 1 BT / màu (ưu tiên Free, default, có ảnh)
IF OBJECT_ID('tempdb..#Keep') IS NOT NULL DROP TABLE #Keep;
;WITH ranked AS (
  SELECT v.Id, v.ProductId, LTRIM(RTRIM(v.Color)) AS Color,
    ROW_NUMBER() OVER (
      PARTITION BY v.ProductId, LTRIM(RTRIM(v.Color))
      ORDER BY
        CASE WHEN v.IsDefault=1 THEN 0 ELSE 1 END,
        CASE WHEN LOWER(LTRIM(RTRIM(ISNULL(v.Size,N'')))) IN (N'free',N'free size') THEN 0 ELSE 1 END,
        CASE WHEN EXISTS (SELECT 1 FROM ProductImages i WHERE i.VariantId=v.Id) THEN 0 ELSE 1 END,
        v.Id
    ) AS rn
  FROM ProductVariants v
  INNER JOIN #Acc a ON a.ProductId = v.ProductId
)
SELECT Id AS VariantId, ProductId, Color INTO #Keep FROM ranked WHERE rn=1;

IF OBJECT_ID('tempdb..#Map') IS NOT NULL DROP TABLE #Map;
SELECT v.Id AS OldVariantId,
  COALESCE(
    (SELECT TOP 1 k.VariantId FROM #Keep k WHERE k.ProductId=v.ProductId AND k.Color=LTRIM(RTRIM(v.Color))),
    (SELECT TOP 1 k.VariantId FROM #Keep k WHERE k.ProductId=v.ProductId ORDER BY k.VariantId)
  ) AS NewVariantId
INTO #Map
FROM ProductVariants v
INNER JOIN #Acc a ON a.ProductId = v.ProductId
WHERE NOT EXISTS (SELECT 1 FROM #Keep k WHERE k.VariantId=v.Id);

UPDATE od SET VariantId=m.NewVariantId FROM OrderDetails od INNER JOIN #Map m ON m.OldVariantId=od.VariantId WHERE m.OldVariantId<>m.NewVariantId;
UPDATE ci SET ProductVariantId=m.NewVariantId FROM CartItems ci INNER JOIN #Map m ON m.OldVariantId=ci.ProductVariantId WHERE m.OldVariantId<>m.NewVariantId;

DELETE i FROM ProductImages i WHERE EXISTS (
  SELECT 1 FROM ProductVariants v INNER JOIN #Acc a ON a.ProductId=v.ProductId
  WHERE v.Id=i.VariantId AND NOT EXISTS (SELECT 1 FROM #Keep k WHERE k.VariantId=v.Id)
);
DELETE v FROM ProductVariants v WHERE EXISTS (
  SELECT 1 FROM #Acc a WHERE a.ProductId=v.ProductId
) AND NOT EXISTS (SELECT 1 FROM #Keep k WHERE k.VariantId=v.Id);

UPDATE v SET Size=N'Free', UpdatedAt=SYSUTCDATETIME()
FROM ProductVariants v INNER JOIN #Keep k ON k.VariantId=v.Id;

UPDATE ProductVariants SET IsDefault=0 WHERE ProductId IN (SELECT ProductId FROM #Acc);
;WITH ranked AS (
  SELECT v.Id, ROW_NUMBER() OVER (
    PARTITION BY v.ProductId
    ORDER BY CASE WHEN EXISTS (SELECT 1 FROM ProductImages i WHERE i.VariantId=v.Id) THEN 0 ELSE 1 END,
             CASE WHEN LTRIM(RTRIM(v.Color)) IN (N'Đen',N'Den') THEN 0 ELSE 1 END, v.Id
  ) rn
  FROM ProductVariants v INNER JOIN #Acc a ON a.ProductId=v.ProductId
)
UPDATE v SET IsDefault=CASE WHEN r.rn=1 THEN 1 ELSE 0 END, DisplayOrder=r.rn, SKU=CONCAT(N'SKU-',v.ProductId,N'-V',r.rn)
FROM ProductVariants v INNER JOIN ranked r ON r.Id=v.Id;

COMMIT;
PRINT '=== DONE: accessory Free size ===';
SELECT p.Id, p.Name, COUNT(v.Id) Vars, STRING_AGG(CAST(v.Size AS NVARCHAR(20)), ',') WITHIN GROUP (ORDER BY v.DisplayOrder) Sizes
FROM Products p JOIN ProductVariants v ON v.ProductId=p.Id
WHERE p.Id IN (SELECT ProductId FROM #Acc)
GROUP BY p.Id, p.Name ORDER BY p.Id;
GO