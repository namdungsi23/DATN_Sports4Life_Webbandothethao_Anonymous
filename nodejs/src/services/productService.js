import { getPool, sql } from "../config/db.js";
import { findAllCategories } from "./categoryService.js";

const SORT_MAP_V2 = {
  name: "p.name",
  price: "p.price",
  id: "p.id",
  available: "p.available",
  createdate: "p.createdate",
};

/** SQL ORDER BY column; matches HomeAPI.resolveSortProperty for filters.sort label */
function resolveHomeSortCol(sort) {
  if (sort == null || String(sort).trim() === "") return { col: "p.price", filterLabel: "price" };
  const s = String(sort).toLowerCase();
  if (s === "id") return { col: "p.id", filterLabel: "id" };
  if (s === "name") return { col: "p.name", filterLabel: "name" };
  if (s === "price") return { col: "p.price", filterLabel: "price" };
  if (s === "available") return { col: "p.available", filterLabel: "available" };
  if (s === "createdate" || s === "create_date")
    return { col: "p.createdate", filterLabel: "createDate" };
  return { col: "p.price", filterLabel: "price" };
}

export function productToPayload(p) {
  const qty = p.invQuantity != null ? p.invQuantity : 0;
  const available = p.available === true || p.available === 1;
  const inStock = available && qty > 0;
  return {
    id: p.id,
    name: p.name,
    image: p.image,
    price: p.price,
    createDate: p.createDate,
    available,
    description: p.description,
    quantity: qty,
    inStock,
    categoryName: p.categoryName != null ? p.categoryName : null,
    categoryId: p.categoryId != null ? p.categoryId : null,
  };
}

function adminToRow(p) {
  const qty = p.invQuantity != null ? p.invQuantity : 0;
  const available = p.available === true || p.available === 1;
  let stockStatus;
  if (available === false) {
    stockStatus = "DISCONTINUED";
  } else if (qty === 0) {
    stockStatus = "OUT";
  } else {
    stockStatus = "IN";
  }
  return {
    id: p.id,
    name: p.name,
    price: p.price,
    quantity: qty,
    categoryName: p.categoryName || "",
    createDate: p.createDate,
    available,
    stockStatus,
  };
}

function buildFilterWhere() {
  return `WHERE (
    (@cat IS NULL OR @cat = N'') OR (cat.name = @cat)
  )
  AND (
    (@keyword IS NULL OR @keyword = N'') OR (LOWER(p.name) LIKE LOWER(N'%' + @keyword + N'%'))
  )
  AND (@min IS NULL OR p.price >= @min)
  AND (@max IS NULL OR p.price <= @max)`;
}

export async function filterProductsPage({ cat, keyword, min, max, page, pageSize, sortCol, sortDir }) {
  const pool = await getPool();
  const where = buildFilterWhere();
  const orderCol = sortCol;
  const orderD = sortDir && String(sortDir).toLowerCase() === "desc" ? "DESC" : "ASC";
  const off = page * pageSize;
  const catP = cat && String(cat).trim() ? String(cat).trim() : null;
  const kw = keyword && String(keyword).trim() ? String(keyword).trim() : null;
  const c = await pool
    .request()
    .input("cat", sql.NVarChar(255), catP)
    .input("keyword", sql.NVarChar(500), kw)
    .input("min", sql.Float, min != null ? min : null)
    .input("max", sql.Float, max != null ? max : null)
    .query(
      `SELECT COUNT(1) AS n
       FROM Products p
       LEFT JOIN Categories cat ON p.Category_Id = cat.Id
       ${where}`
    );
  const totalElements = c.recordset[0].n;
  const totalPages = Math.max(0, Math.ceil(totalElements / pageSize));
  if (pageSize === 0) {
    return { content: [], totalElements, totalPages, number: page, size: pageSize, first: true, last: true };
  }
  const r = await pool
    .request()
    .input("cat", sql.NVarChar(255), catP)
    .input("keyword", sql.NVarChar(500), kw)
    .input("min", sql.Float, min != null ? min : null)
    .input("max", sql.Float, max != null ? max : null)
    .input("off", sql.Int, off)
    .input("lim", sql.Int, pageSize)
    .query(
      `SELECT p.id, p.name, p.image, p.price, p.createdate AS createDate, p.available, p.description,
              cat.name AS categoryName, cat.Id AS categoryId, inv.quantity AS invQuantity
       FROM Products p
       LEFT JOIN Categories cat ON p.Category_Id = cat.Id
       LEFT JOIN INVENTORY inv ON inv.product_id = p.id
       ${where}
       ORDER BY ${orderCol} ${orderD}
       OFFSET @off ROWS FETCH NEXT @lim ROWS ONLY`
    );
  return {
    content: r.recordset,
    totalElements,
    totalPages,
    number: page,
    size: pageSize,
    first: page === 0,
    last: page >= totalPages - 1,
  };
}

export async function getPublicProductsResponse(query) {
  const cat =
    query.cat != null && String(query.cat).trim() !== "" ? String(query.cat).trim() : null;
  const keyword =
    query.keyword != null && String(query.keyword).trim() !== ""
      ? String(query.keyword).trim()
      : null;
  const min = query.min != null && query.min !== "" ? Number(query.min) : null;
  const max = query.max != null && query.max !== "" ? Number(query.max) : null;
  const page = query.page != null && query.page !== "" ? parseInt(String(query.page), 10) : 0;
  const sort = query.sort != null && query.sort !== "" ? String(query.sort) : "price";
  const dir = query.dir != null && query.dir !== "" ? String(query.dir) : "asc";
  const { col: orderCol, filterLabel: sortFilterLabel } = resolveHomeSortCol(sort);
  const sortDir = dir.toLowerCase() === "desc" ? "desc" : "asc";

  const pageRes = await filterProductsPage({
    cat,
    keyword,
    min: Number.isFinite(min) ? min : null,
    max: Number.isFinite(max) ? max : null,
    page,
    pageSize: 10,
    sortCol: orderCol,
    sortDir,
  });

  const content = pageRes.content.map((p) => productToPayload(p));

  const categoryEntities = await findAllCategories();
  const categories = categoryEntities.map((c) => ({
    id: c.id != null ? c.id : "",
    name: c.name != null ? c.name : "",
  }));

  const pageBody = {
    content,
    totalPages: pageRes.totalPages,
    totalElements: pageRes.totalElements,
    number: pageRes.number,
    size: pageRes.size,
    first: pageRes.first,
    last: pageRes.last,
  };

  return {
    products: pageBody,
    categories,
    filters: {
      cat: cat != null && cat !== "" ? cat : "",
      keyword: keyword != null && keyword !== "" ? keyword : "",
      min: min != null && Number.isFinite(min) ? min : 0,
      max: max != null && Number.isFinite(max) ? max : 0,
      sort: sortFilterLabel,
      dir: dir != null && dir !== "" ? dir : "asc",
      page,
    },
  };
}

function resolveV2Sort(sortBy) {
  const s = (sortBy && String(sortBy)) || "name";
  return SORT_MAP_V2[s] || "p.name";
}

export async function filterProductsV2Page({ cat, keyword, min, max, page, pageSize, sortBy, dir }) {
  const orderCol = resolveV2Sort(sortBy);
  const d = dir && String(dir).toLowerCase() === "desc" ? "desc" : "asc";
  return filterProductsPage({
    cat,
    keyword,
    min: min != null && Number.isFinite(Number(min)) ? Number(min) : null,
    max: max != null && Number.isFinite(Number(max)) ? Number(max) : null,
    page,
    pageSize,
    sortCol: orderCol,
    sortDir: d,
  });
}

export async function findProductByIdWithJoins(id) {
  const pool = await getPool();
  const r = await pool
    .request()
    .input("id", sql.Int, id)
    .query(
      `SELECT p.id, p.name, p.image, p.price, p.createdate AS createDate, p.available, p.description,
              cat.Id AS categoryId, cat.name AS categoryName, inv.quantity AS invQuantity, inv.id AS invId, inv.last_updated AS lastUpdated
       FROM Products p
       LEFT JOIN Categories cat ON p.Category_Id = cat.Id
       LEFT JOIN INVENTORY inv ON inv.product_id = p.id
       WHERE p.id = @id`
    );
  return r.recordset[0] || null;
}

function mapToProductEntity(row) {
  if (!row) return null;
  return {
    id: row.id,
    name: row.name,
    image: row.image,
    price: row.price,
    createDate: row.createDate,
    available: row.available,
    description: row.description,
    quantity: 1,
    category:
      row.categoryId != null
        ? { id: row.categoryId, name: row.categoryName }
        : null,
    inventory:
      row.invQuantity != null
        ? {
            id: row.invId,
            quantity: row.invQuantity,
            lastUpdated: row.lastUpdated,
          }
        : { quantity: 0 },
  };
}

export async function getV2ProductResponse({ cat, keyword, min, max, page, pageSize, sortBy, dir }) {
  const res = await filterProductsV2Page({
    cat,
    keyword,
    min,
    max,
    page,
    pageSize,
    sortBy,
    dir,
  });
  const products = res.content.map((row) => mapToProductEntity(row));
  return { products, totalPages: res.totalPages, currentPage: res.number };
}

export async function getAdminProductIndex({ page, size, keyword, cat, editId }) {
  const pool = await getPool();
  const where = buildFilterWhere();
  const off = page * size;
  const catP = cat && String(cat).trim() ? String(cat).trim() : null;
  const kw = keyword && String(keyword).trim() ? String(keyword).trim() : null;
  const c = await pool
    .request()
    .input("cat", sql.NVarChar(255), catP)
    .input("keyword", sql.NVarChar(500), kw)
    .input("min", sql.Float, null)
    .input("max", sql.Float, null)
    .query(
      `SELECT COUNT(1) AS n
       FROM Products p
       LEFT JOIN Categories cat ON p.Category_Id = cat.Id
       ${where}`
    );
  const totalElements = c.recordset[0].n;
  const totalPages = Math.ceil(totalElements / size) || 0;
  const r = await pool
    .request()
    .input("cat", sql.NVarChar(255), catP)
    .input("keyword", sql.NVarChar(500), kw)
    .input("min", sql.Float, null)
    .input("max", sql.Float, null)
    .input("off", sql.Int, off)
    .input("lim", sql.Int, size)
    .query(
      `SELECT p.id, p.name, p.image, p.price, p.createdate AS createDate, p.available, p.description,
              cat.name AS categoryName, cat.Id AS categoryId, inv.quantity AS invQuantity
       FROM Products p
       LEFT JOIN Categories cat ON p.Category_Id = cat.Id
       LEFT JOIN INVENTORY inv ON inv.product_id = p.id
       ${where}
       ORDER BY p.id DESC
       OFFSET @off ROWS FETCH NEXT @lim ROWS ONLY`
    );
  const rows = r.recordset.map((p) => adminToRow(p));
  let product;
  if (editId != null) {
    const full = await findProductByIdWithJoins(Number(editId));
    product = full || {
      id: null,
      name: null,
      price: null,
      description: null,
      available: true,
      image: null,
      invQuantity: 0,
      categoryId: null,
    };
  } else {
    product = {
      id: null,
      name: null,
      price: null,
      description: null,
      available: true,
      image: null,
      invQuantity: 0,
      categoryId: null,
    };
  }
  const form = {
    id: product.id,
    name: product.name,
    price: product.price,
    description: product.description,
    available: product.available != null ? product.available : true,
    image: product.image,
    inventoryQuantity: product.invQuantity != null ? product.invQuantity : 0,
    categoryId: product.categoryId != null && product.categoryId !== "" ? product.categoryId : "",
  };
  const categories = await findAllCategories();
  return {
    products: rows,
    pages: {
      number: page,
      totalPages,
      totalElements,
      size,
    },
    keyword: keyword != null && keyword !== "" ? keyword : "",
    cat: cat != null && cat !== "" ? cat : "",
    categories,
    productForm: form,
  };
}

export async function createOrUpdateProduct(
  { id, name, price, description, available, categoryId, imageUrl },
  inventoryQuantity
) {
  if (inventoryQuantity == null || inventoryQuantity <= 0) {
    const err = new Error("Số lượng phải lớn hơn 0!");
    err.code = "INVALID_INPUT";
    throw err;
  }
  const pool = await getPool();
  const t = new sql.Transaction(pool);
  await t.begin();
  try {
    if (id != null) {
      const trUpd = new sql.Request(t);
      await trUpd
        .input("id", sql.Int, id)
        .input("name", sql.NVarChar(500), name)
        .input("price", sql.Float, price)
        .input("desc", sql.NVarChar(sql.MAX), description ?? null)
        .input("avail", sql.Bit, available !== false)
        .input("img", sql.NVarChar(2000), imageUrl ?? null)
        .input("cid", sql.NVarChar(50), categoryId && String(categoryId).trim() ? categoryId : null)
        .query(
          `UPDATE Products SET name=@name, price=@price, description=@desc, available=@avail, image=@img,
            Category_Id=@cid WHERE id=@id`
        );
      const trSel = new sql.Request(t);
      const ex = await trSel
        .input("pid", sql.Int, id)
        .query(`SELECT id AS invId, quantity FROM INVENTORY WHERE product_id=@pid`);
      const irow = ex.recordset[0];
      if (irow) {
        const tr2 = new sql.Request(t);
        await tr2
          .input("q", sql.Int, inventoryQuantity)
          .input("pid", sql.Int, id)
          .query(
            `UPDATE INVENTORY SET quantity=@q, last_updated=GETDATE() WHERE product_id=@pid`
          );
      } else {
        const tr2 = new sql.Request(t);
        await tr2
          .input("q", sql.Int, inventoryQuantity)
          .input("pid", sql.Int, id)
          .query(
            `INSERT INTO INVENTORY (quantity, product_id, last_updated) VALUES (@q, @pid, GETDATE())`
          );
      }
    } else {
      const ins = new sql.Request(t);
      const insR = await ins
        .input("name", sql.NVarChar(500), name)
        .input("price", sql.Float, price)
        .input("desc", sql.NVarChar(sql.MAX), description ?? null)
        .input("avail", sql.Bit, available !== false)
        .input("img", sql.NVarChar(2000), imageUrl ?? null)
        .input("cid", sql.NVarChar(50), categoryId && String(categoryId).trim() ? categoryId : null)
        .query(
          `INSERT INTO Products (name, image, price, createdate, available, description, Category_Id)
           OUTPUT INSERTED.id
           VALUES (@name, @img, @price, CONVERT(date, GETDATE()), @avail, @desc, @cid)`
        );
      const newId = insR.recordset[0].id;
      const tr2 = new sql.Request(t);
      await tr2
        .input("q", sql.Int, inventoryQuantity)
        .input("pid", sql.Int, newId)
        .query(
          `INSERT INTO INVENTORY (quantity, product_id, last_updated) VALUES (@q, @pid, GETDATE())`
        );
    }
    await t.commit();
  } catch (e) {
    await t.rollback();
    throw e;
  }
}

export async function deleteProductById(id) {
  const pool = await getPool();
  const t = new sql.Transaction(pool);
  await t.begin();
  try {
    const r1 = new sql.Request(t);
    await r1
      .input("id", sql.Int, id)
      .query(`DELETE FROM INVENTORY WHERE product_id=@id`);
    const r2 = new sql.Request(t);
    await r2
      .input("id", sql.Int, id)
      .query(`DELETE FROM OrderDetails WHERE ProductId=@id`);
    const r3 = new sql.Request(t);
    await r3
      .input("id", sql.Int, id)
      .query(`DELETE FROM Products WHERE id=@id`);
    await t.commit();
  } catch (e) {
    await t.rollback();
    throw e;
  }
}
