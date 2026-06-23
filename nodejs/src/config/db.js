import sql from "mssql";
import { config } from "./index.js";

const poolConfig = {
  user: config.db.user,
  password: config.db.password,
  server: config.db.server,
  port: config.db.port,
  database: config.db.database,
  options: {
    ...config.db.options,
  },
  pool: { max: 10, min: 0, idleTimeoutMillis: 30000 },
};

/** @type {import('mssql').ConnectionPool} */
let pool;

export async function getPool() {
  if (!pool) {
    pool = await new sql.ConnectionPool(poolConfig).connect();
  }
  return pool;
}

export { sql };
