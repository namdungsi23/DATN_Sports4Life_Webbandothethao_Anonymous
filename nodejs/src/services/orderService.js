import { getPool, sql } from "../config/db.js";

export async function findAllOrdersSummary() {
  const pool = await getPool();
  const r = await pool.request().query(
    `SELECT o.id, o.createdate AS createDate, o.address, o.status, o.total_amount AS totalAmount,
            a.username
     FROM Orders o
     LEFT JOIN Accounts a ON o.Username = a.username
     ORDER BY o.id DESC`
  );
  return r.recordset.map((o) => ({
    id: o.id,
    username: o.username || "",
    createDate: o.createDate,
    address: o.address,
    status: o.status,
    totalAmount: o.totalAmount,
  }));
}

export async function countTodayOrders() {
  const pool = await getPool();
  const r = await pool.request()
    .query(
      `SELECT COUNT(1) AS n FROM Orders o WHERE o.createdate = CONVERT(date, GETDATE())`
    );
  return r.recordset[0].n;
}

export async function findOrderDetailsByOrderId(orderId) {
  const pool = await getPool();
  const r = await pool
    .request()
    .input("oid", sql.Int, orderId)
    .query(
      `SELECT d.id, d.price, d.quantity, p.name AS productName
       FROM OrderDetails d
       LEFT JOIN Products p ON d.ProductId = p.id
       WHERE d.OrderId = @oid`
    );
  return r.recordset.map((d) => {
    const line =
      (d.price != null ? d.price : 0) * (d.quantity != null ? d.quantity : 0);
    return {
      id: d.id,
      productName: d.productName || "",
      price: d.price,
      quantity: d.quantity,
      lineTotal: line,
    };
  });
}

export async function updateOrderStatus(orderId, status) {
  const pool = await getPool();
  await pool
    .request()
    .input("id", sql.Int, orderId)
    .input("st", sql.NVarChar(50), status)
    .query(`UPDATE Orders SET status=@st WHERE id=@id`);
}
