import { getPool, sql } from "../config/db.js";

export async function findAllCategories() {
  const pool = await getPool();
  const r = await pool.request().query(`SELECT Id AS id, name FROM Categories ORDER BY Id`);
  return r.recordset.map((row) => ({ id: row.id, name: row.name }));
}

export async function findCategoryById(id) {
  const pool = await getPool();
  const r = await pool
    .request()
    .input("id", sql.NVarChar(50), id)
    .query(`SELECT Id AS id, name FROM Categories WHERE Id = @id`);
  return r.recordset[0] || null;
}

export async function countCategories() {
  const pool = await getPool();
  const r = await pool.request().query(`SELECT COUNT(1) AS n FROM Categories`);
  return r.recordset[0].n;
}

function nextCategoryIdFromCount(n) {
  return `CAT${String(Number(n) + 1).padStart(3, "0")}`;
}

export async function generateCategoryId() {
  const n = await countCategories();
  return nextCategoryIdFromCount(n);
}

export async function countProductsInCategory(id) {
  const pool = await getPool();
  const r = await pool
    .request()
    .input("id", sql.NVarChar(50), id)
    .query(`SELECT COUNT(1) AS n FROM Products WHERE Category_Id = @id`);
  return r.recordset[0].n;
}

export async function createCategoryByName(name) {
  if (await existsName(name)) {
    const err = new Error("Tên danh mục đã tồn tại");
    err.code = "DUPLICATE";
    throw err;
  }
  const id = await generateCategoryId();
  const pool = await getPool();
  await pool
    .request()
    .input("id", sql.NVarChar(50), id)
    .input("name", sql.NVarChar(255), name)
    .query(`INSERT INTO Categories (Id, name) VALUES (@id, @name)`);
  return { id, name };
}

export async function existsName(name) {
  const pool = await getPool();
  const r = await pool
    .request()
    .input("n", sql.NVarChar(255), name)
    .query(`SELECT COUNT(1) AS c FROM Categories WHERE name = @n`);
  return r.recordset[0].c > 0;
}

export async function updateCategoryName(id, name) {
  const pool = await getPool();
  await pool
    .request()
    .input("id", sql.NVarChar(50), id)
    .input("name", sql.NVarChar(255), name)
    .query(`UPDATE Categories SET name=@name WHERE Id=@id`);
}

export async function deleteCategoryById(id) {
  const n = await countProductsInCategory(id);
  if (n > 0) {
    const err = new Error("Danh mục đang chứa sản phẩm");
    err.code = "CONSTRAINT";
    throw err;
  }
  const pool = await getPool();
  await pool
    .request()
    .input("id", sql.NVarChar(50), id)
    .query(`DELETE FROM Categories WHERE Id = @id`);
}

/** Admin category list index — mirror AdminCategoryApiController */
export async function categoryIndex({ keyword, size, page }) {
  const pool = await getPool();
  if (size == null || size === "all") {
    const list = await findAllCategories();
    return {
      categories: list,
      totalPages: 1,
      currentPage: page,
      keyword: keyword ?? "",
      size: size ?? "all",
    };
  }
  const pageSize = parseInt(String(size), 10) || 5;
  const k = keyword && keyword.trim() ? keyword.trim() : "";
  const off = page * pageSize;
  // Mirror CategoryServiceImpl.search: findByNameContainingIgnoreCase
  if (!k) {
    const c = await pool.request().query(`SELECT COUNT(1) AS n FROM Categories`);
    const total = c.recordset[0].n;
    const totalPages = Math.max(1, Math.ceil(total / pageSize));
    const r = await pool
      .request()
      .input("off", sql.Int, off)
      .input("lim", sql.Int, pageSize)
      .query(
        `SELECT Id AS id, name FROM Categories ORDER BY Id
         OFFSET @off ROWS FETCH NEXT @lim ROWS ONLY`
      );
    return {
      categories: r.recordset,
      totalPages,
      currentPage: page,
      keyword: "",
      size: String(pageSize),
    };
  }
  const like = `%${k}%`;
  const c = await pool
    .request()
    .input("k2", sql.NVarChar(255), like)
    .query(
      `SELECT COUNT(1) AS n FROM Categories WHERE LOWER(name) LIKE LOWER(@k2)`
    );
  const total = c.recordset[0].n;
  const totalPages = Math.max(1, Math.ceil(total / pageSize));
  const r = await pool
    .request()
    .input("k2", sql.NVarChar(255), like)
    .input("off", sql.Int, off)
    .input("lim", sql.Int, pageSize)
    .query(
      `SELECT Id AS id, name FROM Categories
       WHERE LOWER(name) LIKE LOWER(@k2)
       ORDER BY Id
       OFFSET @off ROWS FETCH NEXT @lim ROWS ONLY`
    );
  return {
    categories: r.recordset,
    totalPages,
    currentPage: page,
    keyword: k,
    size: String(pageSize),
  };
}

/**
 * filterCategories for admin v2 — keyword NULL or '' or id match or name like
 */
export async function filterCategoriesV2Page(keyword, page, pageSize, sortBy, dir) {
  const pool = await getPool();
  const colMap = { name: "c.name", id: "c.Id" };
  const orderCol = colMap[sortBy] || "c.name";
  const orderDir = dir && String(dir).toLowerCase() === "desc" ? "DESC" : "ASC";
  const kw = keyword != null && String(keyword).trim() !== "" ? String(keyword).trim() : null;
  const off = page * pageSize;
  if (kw == null) {
    const c = await pool.request().query(`SELECT COUNT(1) AS n FROM Categories c`);
    const total = c.recordset[0].n;
    const totalPages = Math.max(1, Math.ceil(total / pageSize)) || 1;
    const r = await pool
      .request()
      .input("off", sql.Int, off)
      .input("lim", sql.Int, pageSize)
      .query(
        `SELECT c.Id AS id, c.name
         FROM Categories c
         ORDER BY ${orderCol} ${orderDir}
         OFFSET @off ROWS FETCH NEXT @lim ROWS ONLY`
      );
    return { content: r.recordset, totalPages, number: page };
  }
  const like = `%${kw}%`;
  const c = await pool
    .request()
    .input("k1", sql.NVarChar(50), kw)
    .input("k2", sql.NVarChar(255), like)
    .query(
      `SELECT COUNT(1) AS n FROM Categories c
       WHERE c.Id = @k1 OR c.name LIKE @k2`
    );
  const total = c.recordset[0].n;
  const totalPages = Math.max(1, Math.ceil(total / pageSize)) || 1;
  const r = await pool
    .request()
    .input("k1", sql.NVarChar(50), kw)
    .input("k2", sql.NVarChar(255), like)
    .input("off", sql.Int, off)
    .input("lim", sql.Int, pageSize)
    .query(
      `SELECT c.Id AS id, c.name
       FROM Categories c
       WHERE c.Id = @k1 OR c.name LIKE @k2
       ORDER BY ${orderCol} ${orderDir}
       OFFSET @off ROWS FETCH NEXT @lim ROWS ONLY`
    );
  return { content: r.recordset, totalPages, number: page };
}
