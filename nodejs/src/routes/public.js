import { Router } from "express";
import { getPublicProductsResponse } from "../services/productService.js";

const r = Router();

r.get("/", (req, res) => {
  res.json({ message: "API is running", status: "ok" });
});

r.get("/products", async (req, res) => {
  try {
    const body = await getPublicProductsResponse(req.query);
    res.json(body);
  } catch (e) {
    console.error(e);
    res.status(500).json({ message: "Internal server error" });
  }
});

export default r;
