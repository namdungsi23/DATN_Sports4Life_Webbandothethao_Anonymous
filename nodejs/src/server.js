import express from "express";
import cors from "cors";
import { config } from "./config/index.js";
import publicRoutes from "./routes/public.js";
import authRoutes from "./routes/auth.js";
import adminRoutes from "./routes/admin.js";
import adminV2Routes from "./routes/adminV2.js";

const app = express();

app.use(
  cors({
    origin: config.corsOrigins,
    credentials: true,
    methods: ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
  })
);

app.use(express.urlencoded({ extended: true }));
app.use(express.json());

app.use("/api/public", publicRoutes);
app.use("/", authRoutes);
app.use("/api/admin/v2", adminV2Routes);
app.use("/api/admin", adminRoutes);

app.use((err, req, res, next) => {
  console.error(err);
  if (res.headersSent) {
    next(err);
    return;
  }
  res.status(500).json({ message: "Internal server error" });
});

app.listen(config.port, () => {
  console.log(`ASSM Node mirror listening on http://localhost:${config.port}`);
});
