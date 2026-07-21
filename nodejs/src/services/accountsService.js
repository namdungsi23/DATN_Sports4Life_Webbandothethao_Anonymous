import { getPool, sql } from "../config/db.js";
import { normalizeRole } from "../utils/springRoles.js";

export async function findAccountByUsername(username) {
  const pool = await getPool();
  const r = await pool
    .request()
    .input("u", sql.NVarChar(255), username)
    .query(
      `SELECT username, password, fullname, email, photo, activated, admin
       FROM Accounts WHERE username = @u`
    );
  return r.recordset[0] || null;
}

export async function loadRoleNamesForUser(username) {
  const pool = await getPool();
  const r = await pool
    .request()
    .input("u", sql.NVarChar(255), username)
    .query(
      `SELECT r.name
       FROM User_Role ur
       INNER JOIN Roles r ON ur.RoleId = r.id
       WHERE ur.Username = @u`
    );
  return r.recordset.map((row) => normalizeRole(String(row.name)));
}

/** Returns authorities like Spring UserDetails: ROLE_* list */
export async function loadAuthorityStrings(username) {
  const pool = await getPool();
  const r = await pool
    .request()
    .input("u", sql.NVarChar(255), username)
    .query(
      `SELECT r.name
       FROM User_Role ur
       INNER JOIN Roles r ON ur.RoleId = r.id
       WHERE ur.Username = @u`
    );
  return r.recordset.map((row) => normalizeRole(String(row.name)));
}

function accountToJson(row) {
  if (!row) return null;
  return {
    username: row.username,
    fullname: row.fullname,
    email: row.email,
    photo: row.photo,
    activated: row.activated,
    admin: row.admin,
  };
}

export async function findAllAccountsPaged(page, size) {
  const pool = await getPool();
  const off = page * size;
  const c = await pool.request().query(`SELECT COUNT(1) AS n FROM Accounts`);
  const total = c.recordset[0].n;
  const totalPages = size > 0 ? Math.ceil(total / size) : 0;
  const data = await pool
    .request()
    .input("off", sql.Int, off)
    .input("lim", sql.Int, size)
    .query(
      `SELECT username, fullname, email, photo, activated, admin
       FROM Accounts
       ORDER BY username
       OFFSET @off ROWS FETCH NEXT @lim ROWS ONLY`
    );
  return {
    content: data.recordset.map(accountToJson),
    number: page,
    totalPages,
    totalElements: total,
    size,
    first: page === 0,
    last: page >= totalPages - 1,
  };
}

export async function searchAccountsPaged(keyword, page, size) {
  const pool = await getPool();
  const k = `%${keyword}%`;
  const c = await pool
    .request()
    .input("k", sql.NVarChar(255), k)
    .query(
      `SELECT COUNT(1) AS n FROM Accounts
       WHERE username LIKE @k OR fullname LIKE @k OR email LIKE @k`
    );
  const total = c.recordset[0].n;
  const totalPages = size > 0 ? Math.ceil(total / size) : 0;
  const off = page * size;
  const data = await pool
    .request()
    .input("k", sql.NVarChar(255), k)
    .input("off", sql.Int, off)
    .input("lim", sql.Int, size)
    .query(
      `SELECT username, fullname, email, photo, activated, admin
       FROM Accounts
       WHERE username LIKE @k OR fullname LIKE @k OR email LIKE @k
       ORDER BY username
       OFFSET @off ROWS FETCH NEXT @lim ROWS ONLY`
    );
  return {
    content: data.recordset.map(accountToJson),
    number: page,
    totalPages,
    totalElements: total,
    size,
    first: page === 0,
    last: page >= totalPages - 1,
  };
}

export async function updateAccountPartial({ username, fullname, email, photo, activated }) {
  const pool = await getPool();
  const existing = await findAccountByUsername(username);
  if (!existing) return false;
  await pool
    .request()
    .input("u", sql.NVarChar(255), username)
    .input("fn", sql.NVarChar(255), fullname ?? existing.fullname)
    .input("em", sql.NVarChar(255), email ?? existing.email)
    .input("ph", sql.NVarChar(500), photo ?? existing.photo)
    .input("ac", sql.Bit, activated ?? existing.activated)
    .query(
      `UPDATE Accounts SET fullname=@fn, email=@em, photo=@ph, activated=@ac WHERE username=@u`
    );
  return true;
}

export async function allRoleNames() {
  const pool = await getPool();
  const r = await pool.request().query(`SELECT name FROM Roles ORDER BY name`);
  return r.recordset.map((x) => x.name);
}
