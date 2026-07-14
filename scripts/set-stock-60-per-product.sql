-- Mỗi SP: tổng kho = 60
-- Chia đều theo màu (thường 4 màu × 15); trong mỗi màu chia đều theo size.
SET XACT_ABORT ON;
SET NOCOUNT ON;
BEGIN TRANSACTION;

;WITH ColorCounts AS (
    SELECT
        ProductId,
        LTRIM(RTRIM(Color)) AS Color,
        COUNT(*) AS SizeCnt
    FROM ProductVariants
    GROUP BY ProductId, LTRIM(RTRIM(Color))
),
ProductColorCnt AS (
    SELECT ProductId, COUNT(*) AS ColorCnt
    FROM ColorCounts
    GROUP BY ProductId
),
ColorQuota AS (
    SELECT
        cc.ProductId,
        cc.Color,
        cc.SizeCnt,
        pc.ColorCnt,
        -- tổng 60 chia đều theo số màu; dư cộng vào màu đầu (theo tên)
        (60 / pc.ColorCnt)
            + CASE
                WHEN ROW_NUMBER() OVER (PARTITION BY cc.ProductId ORDER BY cc.Color)
                     <= (60 % pc.ColorCnt)
                THEN 1 ELSE 0
              END AS ColorStock
    FROM ColorCounts cc
    JOIN ProductColorCnt pc ON pc.ProductId = cc.ProductId
),
Sized AS (
    SELECT
        v.Id AS VariantId,
        cq.ColorStock,
        cq.SizeCnt,
        ROW_NUMBER() OVER (
            PARTITION BY v.ProductId, LTRIM(RTRIM(v.Color))
            ORDER BY ISNULL(v.DisplayOrder, 9999), v.Id
        ) AS SizeRn
    FROM ProductVariants v
    JOIN ColorQuota cq
      ON cq.ProductId = v.ProductId
     AND cq.Color = LTRIM(RTRIM(v.Color))
)
UPDATE v
SET
    v.Quantity = CAST(
        (s.ColorStock / s.SizeCnt)
        + CASE WHEN s.SizeRn <= (s.ColorStock % s.SizeCnt) THEN 1 ELSE 0 END
        AS smallint
    ),
    v.UpdatedAt = SYSUTCDATETIME()
FROM ProductVariants v
JOIN Sized s ON s.VariantId = v.Id;

COMMIT;

-- Kiểm tra nhanh
SELECT
    COUNT(*) AS products,
    MIN(totalQty) AS minStock,
    MAX(totalQty) AS maxStock,
    AVG(1.0 * totalQty) AS avgStock
FROM (
    SELECT ProductId, SUM(CAST(Quantity AS int)) AS totalQty
    FROM ProductVariants
    GROUP BY ProductId
) t;

SELECT TOP 3
    p.Id,
    p.Name,
    LTRIM(RTRIM(v.Color)) AS Color,
    SUM(CAST(v.Quantity AS int)) AS colorStock,
    COUNT(*) AS sizes
FROM Products p
JOIN ProductVariants v ON v.ProductId = p.Id
WHERE p.Id IN (1, 61, 7)
GROUP BY p.Id, p.Name, LTRIM(RTRIM(v.Color))
ORDER BY p.Id, Color;
