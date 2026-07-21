import { Router } from "express";
import { requireAdmin } from "../middleware/auth.js";
import { filterCategoriesV2Page } from "../services/categoryService.js";
import { getV2ProductResponse } from "../services/productService.js";

const r = Router();
r.use(requireAdmin);

r.get("/categories", async (req, res) => {
  const keyword = req.query.keyword;
  const page = parseInt(String(req.query.page ?? "0"), 10) || 0;
  const pageSize = parseInt(String(req.query.pageSize ?? "10"), 10) || 10;
  const dir = req.query.dir ?? "asc";
  const sortBy = (req.query.sortBy ?? "name") + "";
  const { content, totalPages, number } = await filterCategoriesV2Page(
    keyword,
    page,
    pageSize,
    sortBy,
    dir
  );
  res.json({ categories: content, totalPages, currentPage: number });
});

r.get("/products", async (req, res) => {
  const {
    cat,
    keyword,
    min,
    max,
    page: pageQ,
    pageSize: pageSizeQ,
    dir,
    sortBy,
  } = req.query;
  const page = parseInt(String(pageQ ?? "0"), 10) || 0;
  const pageSize = parseInt(String(pageSizeQ ?? "10"), 10) || 10;
  const out = await getV2ProductResponse({
    cat: cat || undefined,
    keyword: keyword || undefined,
    min: min != null && min !== "" ? Number(min) : null,
    max: max != null && max !== "" ? Number(max) : null,
    page,
    pageSize,
    sortBy: (sortBy ?? "name") + "",
    dir: (dir ?? "asc") + "",
  });
  res.json({
    products: out.products,
    totalPages: out.totalPages,
    currentPage: out.currentPage,
  });
});

export default r;
