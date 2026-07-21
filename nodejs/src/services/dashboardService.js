import { getPool, sql } from "../config/db.js";

export async function getDashboardStats() {
  const pool = await getPool();
  const totalUsers = (await pool.request().query(`SELECT COUNT(1) AS n FROM Accounts`))
    .recordset[0].n;
  const totalProducts = (await pool.request().query(`SELECT COUNT(1) AS n FROM Products`))
    .recordset[0].n;
  const totalOrders = (await pool.request().query(`SELECT COUNT(1) AS n FROM Orders`))
    .recordset[0].n;
  const todayOrders = (
    await pool.request()
      .query(
        `SELECT COUNT(1) AS n FROM Orders o WHERE o.createdate = CONVERT(date, GETDATE())`
      )
  ).recordset[0].n;
  const weekAgo = new Date();
  weekAgo.setDate(weekAgo.getDate() - 7);
  const newProducts = (
    await pool
      .request()
      .input("d", sql.Date, weekAgo)
      .query(
        `SELECT COUNT(1) AS n FROM Products p WHERE p.createdate >= @d`
      )
  ).recordset[0].n;

  return {
    totalUsers,
    totalProducts,
    totalOrders,
    todayOrders,
    newProducts,
  };
}
