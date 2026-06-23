<template>
    <AdminLayout>
      <h4>➕ Thêm /Sửa sản phẩm</h4>
      <div v-if="flashOk" class="alert alert-success">{{ flashOk }}</div>
      <div v-if="flashErr" class="alert alert-danger">{{ flashErr }}</div>
      <div v-if="err" class="alert alert-danger">{{ err }}</div>
  
      <form class="border p-3 mb-4" @submit.prevent="saveProduct">
        <input type="hidden" :value="productForm.id" />
        <div class="row">
          <div class="col-md-6 mb-3">
            <label>Tên sản phẩm</label>
            <input v-model="productForm.name" class="form-control" required />
          </div>
          <div class="col-md-3 mb-3">
            <label>Giá</label>
            <input v-model.number="productForm.price" type="number" step="0.01" class="form-control" required />
          </div>
          <div class="col-md-3 mb-3">
            <label>Số lượng</label>
            <input v-model.number="productForm.inventoryQuantity" type="number" class="form-control" required />
          </div>
          <div class="col-md-3 mb-3">
            <label>Danh mục</label>
            <select v-model="productForm.categoryId" class="form-select">
              <option value="">-- Chọn danh mục --</option>
              <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </div>
        </div>
        <div class="mb-3">
          <label>Hình ảnh</label>
          <div v-if="productForm.image">
            <img :src="productForm.image" alt="" style="max-width: 150px; margin-bottom: 10px" />
          </div>
          <input type="file" class="form-control" accept="image/*" @change="onFile" />
        </div>
        <div class="mb-3">
          <label>Mô tả</label>
          <textarea v-model="productForm.description" class="form-control" rows="3" />
        </div>
        <div class="form-check mb-3">
          <input id="av" v-model="productForm.available" class="form-check-input" type="checkbox" />
          <label class="form-check-label" for="av">Còn hàng</label>
        </div>
        <button type="submit" class="btn btn-primary">Lưu sản phẩm</button>
        <button type="button" class="btn btn-secondary ms-2" @click="resetForm">Reset</button>
      </form>
  
      <form class="row mb-3" @submit.prevent="doFilter">
        <div class="col-md-4">
          <input v-model="filterKeyword" class="form-control" placeholder="Tìm sản phẩm" />
        </div>
        <div class="col-md-3">
          <select v-model="filterCat" class="form-select">
            <option value="">-- Danh mục --</option>
            <option v-for="c in categories" :key="'f-' + c.id" :value="c.id">{{ c.name }}</option>
          </select>
        </div>
        <div class="col-md-2">
          <button type="submit" class="btn btn-dark w-100">Tìm</button>
        </div>
      </form>
  
      <table class="table table-bordered text-center align-middle">
        <thead class="table-dark">
          <tr>
            <th>ID</th>
            <th>Tên</th>
            <th>Giá</th>
            <th>Hàng tồn kho</th>
            <th>Danh mục</th>
            <th>Ngày tạo</th>
            <th>Trạng thái</th>
            <th>Hành động</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in products" :key="p.id">
            <td>{{ p.id }}</td>
            <td>{{ p.name }}</td>
            <td>{{ formatPrice(p.price) }}</td>
            <td>{{ p.quantity }}</td>
            <td>{{ p.categoryName }}</td>
            <td>{{ p.createDate }}</td>
            <td>
              <span v-if="p.stockStatus === 'DISCONTINUED'" class="badge bg-secondary">Ngừng kinh doanh</span>
              <span v-else-if="p.stockStatus === 'OUT'" class="badge bg-danger">Hết hàng</span>
              <span v-else class="badge bg-success">Còn</span>
            </td>
            <td>
              <button type="button" class="btn btn-warning btn-sm me-1" @click="loadEdit(p.id)">Sửa</button>
              <button type="button" class="btn btn-danger btn-sm" @click="remove(p.id)">Xóa</button>
            </td>
          </tr>
        </tbody>
      </table>
  
      <ul class="pagination justify-content-center">
        <li v-for="i in pageCount" :key="i" class="page-item">
          <a class="page-link" href="#" @click.prevent="goPage(i - 1)">{{ i }}</a>
        </li>
      </ul>
    </AdminLayout>
  </template>
  
  <script setup>
  import { computed, onMounted, reactive, ref } from "vue";
  import AdminLayout from "../../layouts/AdminLayout.vue";
  import { API_BASE, apiFetch } from "../../services/http.js";
  
  const err = ref("");
  const flashOk = ref("");
  const flashErr = ref("");
  const products = ref([]);
  const categories = ref([]);
  const pageInfo = reactive({ number: 0, totalPages: 1 });
  const filterKeyword = ref("");
  const filterCat = ref("");
  const fileRef = ref(null);
  const productForm = reactive({
    id: null,
    name: "",
    price: 0,
    inventoryQuantity: 0,
    categoryId: "",
    description: "",
    available: true,
    image: "",
  });
  
  const pageCount = computed(() => Math.max(1, pageInfo.totalPages || 1));
  
  function formatPrice(n) {
    return Number(n || 0).toLocaleString("vi-VN");
  }
  
  function onFile(e) {
    const f = e.target.files?.[0];
    fileRef.value = f || null;
  }
  
  function applyForm(f) {
    productForm.id = f.id ?? null;
    productForm.name = f.name || "";
    productForm.price = f.price ?? 0;
    productForm.inventoryQuantity = f.inventoryQuantity ?? 0;
    productForm.categoryId = f.categoryId || "";
    productForm.description = f.description || "";
    productForm.available = f.available !== false;
    productForm.image = f.image || "";
  }
  
  async function load(page = 0, editId = null) {
    err.value = "";
    const params = new URLSearchParams({
      page: String(page),
      keyword: filterKeyword.value,
      cat: filterCat.value,
      size: "10",
    });

    if (editId != null) params.set("editId", String(editId));
    const data = await apiFetch(`/api/admin/products?${params}`);
    products.value = data.products || [];
    categories.value = data.categories || [];
    pageInfo.number = data.pages?.number ?? 0;
    pageInfo.totalPages = data.pages?.totalPages ?? 1;
    applyForm(data.productForm || {});
  }
  
  function resetForm() {
    fileRef.value = null;
    load(0, null).catch((e) => {
      err.value = e.message || "Lỗi";
    });
  }
  
  function doFilter() {
    load(0).catch((e) => {
      err.value = e.message || "Lỗi";
    });
  }
  
  function goPage(p) {
    load(p).catch((e) => {
      err.value = e.message || "Lỗi";
    });
  }
  
  function loadEdit(id) {
    load(pageInfo.number, id).catch((e) => {
      err.value = e.message || "Lỗi";
    });
  }
  
  async function saveProduct() {
    flashOk.value = "";
    flashErr.value = "";
    const fd = new FormData();
    if (productForm.id != null) fd.append("id", String(productForm.id));
    fd.append("name", productForm.name);
    fd.append("price", String(productForm.price));
    fd.append("inventory.quantity", String(productForm.inventoryQuantity));
    if (productForm.categoryId) fd.append("categoryId", productForm.categoryId);
    fd.append("description", productForm.description || "");
    fd.append("available", productForm.available ? "true" : "false");
    if (fileRef.value) fd.append("uploadImage", fileRef.value);
  
    try {
      const data = await apiFetch(`${API_BASE}/api/admin/products/save`, {
        method: "POST",
        body: fd,
      });
      flashOk.value = data.message || "Lưu thành công!";
      fileRef.value = null;
      await load(pageInfo.number);
    } catch (e) {
      flashErr.value = e.message || "Lỗi lưu";
    }
  }
  
  async function remove(id) {
    if (!confirm("Xóa sản phẩm?")) return;
    try {
      const res = await apiFetch(`/api/admin/products/${id}`, { method: "DELETE" });
      flashOk.value = res.message || "Đã xóa";
      await load(pageInfo.number);
    } catch (e) {
      flashErr.value = e.message || "Xóa thất bại";
    }
  }
  
  onMounted(() => {
    load(0).catch((e) => {
      err.value = e.message || "Lỗi tải";
    });
  });
  </script>
  