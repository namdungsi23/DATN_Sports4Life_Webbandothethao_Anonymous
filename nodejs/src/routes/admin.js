import { Router } from "express";
import multer from "multer";
import { requireAdmin } from "../middleware/auth.js";
import {
  findAllAccountsPaged,
  searchAccountsPaged,
  findAccountByUsername,
  allRoleNames,
  updateAccountPartial,
} from "../services/accountsService.js";
import { getDashboardStats } from "../services/dashboardService.js";
import { categoryIndex, findCategoryById, createCategoryByName, updateCategoryName, deleteCategoryById } from "../services/categoryService.js";
import {
  getAdminProductIndex,
  createOrUpdateProduct,
  findProductByIdWithJoins,
  deleteProductById,
} from "../services/productService.js";
import { uploadProductImage } from "../services/cloudinaryService.js";
import {
  findAllOrdersSummary,
  countTodayOrders,
  findOrderDetailsByOrderId,
  updateOrderStatus,
} from "../services/orderService.js";

const r = Router();
r.use(requireAdmin);

r.get("/users", async (req, res) => {
  const keyword = (req.query.keyword ?? "") + "";
  const page = parseInt(String(req.query.page ?? "0"), 10) || 0;
  const size = 5;
  const pages = keyword
    ? await searchAccountsPaged(keyword, page, size)
    : await findAllAccountsPaged(page, size);
  const authorities = await allRoleNames();
  res.json({
    users: pages.content,
    pages: {
      number: pages.number,
      totalPages: pages.totalPages,
      totalElements: pages.totalElements,
      size: pages.size,
      first: pages.first,
      last: pages.last,
    },
    keyword,
    authorities,
  });
});

r.get("/users/:username", async (req, res) => {
  const username = req.params.username;
  const user = await findAccountByUsername(decodeURIComponent(username));
  if (!user) {
    return res.status(404).end();
  }
  const listPayload = await (async () => {
    const keyword = (req.query.keyword ?? "") + "";
    const page = parseInt(String(req.query.page ?? "0"), 10) || 0;
    const size = 5;
    return keyword
      ? await searchAccountsPaged(keyword, page, size)
      : await findAllAccountsPaged(page, size);
  })();
  const authorities = await allRoleNames();
  const { password, ...safe } = user;
  res.json({
    user: safe,
    users: listPayload.content,
    pages: {
      number: listPayload.number,
      totalPages: listPayload.totalPages,
      totalElements: listPayload.totalElements,
      size: listPayload.size,
      first: listPayload.first,
      last: listPayload.last,
    },
    keyword: (req.query.keyword ?? "") + "",
    authorities,
  });
});

r.post("/users/save", async (req, res) => {
  const b = req.body;
  if (!b || !b.username || String(b.username).trim() === "") {
    return res.status(400).json({ ok: false, message: "Thiếu username" });
  }
  const target = await findAccountByUsername(String(b.username).trim());
  if (!target) {
    return res.status(400).json({ ok: false, message: "Không tìm thấy người dùng" });
  }
  await updateAccountPartial({
    username: b.username,
    fullname: b.fullname,
    email: b.email,
    photo: b.photo,
    activated: b.activated,
  });
  res.json({ ok: true, message: "Cập nhật thành công" });
});

r.get("/dashboard", async (req, res) => {
  res.json(await getDashboardStats());
});

r.get("/categories", async (req, res) => {
  res.json(
    await categoryIndex({
      keyword: req.query.keyword,
      size: req.query.size,
      page: parseInt(String(req.query.page ?? "0"), 10) || 0,
    })
  );
});

r.get("/categories/:id", async (req, res) => {
  try {
    const c = await findCategoryById(req.params.id);
    if (!c) return res.status(404).end();
    res.json(c);
  } catch {
    return res.status(404).end();
  }
});

r.post("/categories", async (req, res) => {
  try {
    await createCategoryByName(req.body?.name);
    res.json({ ok: true, message: "Lưu thành công!" });
  } catch (e) {
    if (e.code === "DUPLICATE") {
      return res.status(400).json({ ok: false, message: e.message || "Lưu thất bại!" });
    }
    return res.status(400).json({ ok: false, message: "Lưu thất bại!" });
  }
});

r.put("/categories", async (req, res) => {
  const c = req.body;
  if (!c || !c.id) {
    return res.status(400).json({ ok: false, message: "Thiếu mã danh mục" });
  }
  try {
    await updateCategoryName(c.id, c.name);
    return res.json({ ok: true, message: "Cập nhật thành công" });
  } catch (e) {
    return res.status(400).json({ ok: false, message: "Cập nhật thất bại" });
  }
});

r.delete("/categories/:id", async (req, res) => {
  try {
    await deleteCategoryById(req.params.id);
    return res.json({ ok: true, message: "Đã xóa thành công!" });
  } catch (e) {
    return res
      .status(400)
      .json({ ok: false, message: e.message || "Xóa thất bại!" });
  }
});

const upload = multer({ storage: multer.memoryStorage() });

r.get("/products", async (req, res) => {
  res.json(
    await getAdminProductIndex({
      page: parseInt(String(req.query.page ?? "0"), 10) || 0,
      size: parseInt(String(req.query.size ?? "10"), 10) || 10,
      keyword: (req.query.keyword ?? "") + "",
      cat: (req.query.cat ?? "") + "",
      editId: req.query.editId != null ? parseInt(String(req.query.editId), 10) : null,
    })
  );
});

r.post(
  "/products/save",
  upload.single("uploadImage"),
  async (req, res) => {
    const id = req.body.id != null && req.body.id !== "" ? parseInt(req.body.id, 10) : null;
    const name = req.body.name;
    const price = parseFloat(req.body.price);
    const inventoryQuantity = parseInt(
      req.body["inventory.quantity"] ?? req.body.inventoryQuantity,
      10
    );
    const categoryId = req.body.categoryId;
    const description = req.body.description;
    const available = req.body.available !== "false" && req.body.available !== false;
    if (!name || Number.isNaN(price)) {
      return res.status(400).json({ ok: false, message: "Invalid data" });
    }
    let imageUrl;
    if (req.file) {
      try {
        imageUrl = await uploadProductImage(req.file.buffer, req.file.originalname);
      } catch (e) {
        console.error(e);
        return res
          .status(400)
          .json({ ok: false, message: e.message || "Upload failed" });
      }
    } else if (id != null) {
      const existing = await findProductByIdWithJoins(id);
      if (existing) {
        imageUrl = existing.image ?? null;
      }
    }
    try {
      await createOrUpdateProduct(
        {
          id,
          name,
          price,
          description,
          available,
          categoryId: categoryId || null,
          imageUrl,
        },
        Number.isInteger(inventoryQuantity) ? inventoryQuantity : null
      );
      return res.json({ ok: true, message: "Lưu thành công!" });
    } catch (e) {
      if (e.code === "INVALID_INPUT") {
        return res.status(400).json({ ok: false, message: e.message });
      }
      console.error(e);
      return res.status(500).json({ ok: false, message: "Lưu thất bại!" });
    }
  }
);

r.delete("/products/:id", async (req, res) => {
  try {
    await deleteProductById(parseInt(req.params.id, 10));
    return res.json({ ok: true, message: "Xóa sản phẩm thành công!" });
  } catch (e) {
    return res
      .status(400)
      .json({ ok: false, message: "Xóa thất bại!" });
  }
});

r.get("/orders", async (req, res) => {
  res.json({
    orders: await findAllOrdersSummary(),
    todayCount: await countTodayOrders(),
  });
});

r.get("/orders/:id", async (req, res) => {
  const id = parseInt(req.params.id, 10);
  res.json({
    orders: await findAllOrdersSummary(),
    todayCount: await countTodayOrders(),
    selectedOrderId: id,
    orderDetails: await findOrderDetailsByOrderId(id),
  });
});

r.post("/orders/update-status", async (req, res) => {
  const b = req.body;
  if (b == null) {
    return res.status(400).json({ ok: false });
  }
  const orderId = b.orderId;
  const st = b.status;
  if (orderId == null || st == null) {
    return res.status(400).json({ ok: false });
  }
  await updateOrderStatus(Number(orderId), String(st));
  return res.json({ ok: true });
});

export default r;
