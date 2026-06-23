import dotenv from "dotenv";

dotenv.config();

const num = (v, d) => {
  const n = Number(v);
  return Number.isFinite(n) ? n : d;
};

export const config = {
  port: num(process.env.PORT, 3001),
  jwtSecret: process.env.JWT_SECRET || "0123456789.0123456789.0123456789",
  jwtAccessExpirySeconds: num(process.env.JWT_ACCESS_EXPIRY_SECONDS, 15 * 60),
  db: {
    server: process.env.DB_SERVER || "localhost",
    port: num(process.env.DB_PORT, 1433),
    database: process.env.DB_DATABASE || "Java5PlaySport1",
    user: process.env.DB_USER || "sa",
    password: process.env.DB_PASSWORD || "",
    options: {
      encrypt: String(process.env.DB_ENCRYPT ?? "true") === "true",
      trustServerCertificate: String(process.env.DB_TRUST_SERVER_CERTIFICATE ?? "true") === "true",
      enableArithAbort: true,
    },
  },
  cloudinary: {
    cloudName: process.env.CLOUDINARY_CLOUD_NAME,
    apiKey: process.env.CLOUDINARY_API_KEY,
    apiSecret: process.env.CLOUDINARY_API_SECRET,
  },
  corsOrigins: [
    "http://localhost:5173",
    "http://localhost:5174",
    "http://127.0.0.1:5173",
    "http://127.0.0.1:5174",
  ],
};
