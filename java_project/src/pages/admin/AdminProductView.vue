<template>
  <AdminLayout>
    <AdminReadOnlyNotice />
    <h4>Quản lý sản phẩm & biến thể</h4>
    <div v-if="flashOk" class="alert alert-success">{{ flashOk }}</div>
    <div v-if="flashErr" class="alert alert-danger">{{ flashErr }}</div>
    <div v-if="err" class="alert alert-danger">{{ err }}</div>

    <!-- Top panel: variant editor -->
    <section class="border rounded p-3 mb-3 bg-light">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <h5 class="mb-0">Biên tập biến thể</h5>
        <span v-if="numericProductId()" class="text-muted small">Sản phẩm #{{ numericProductId() }}</span>
        <span v-else-if="selectedProductId === 'new'" class="text-muted small">Sản phẩm mới</span>
      </div>

      <div v-if="!selectedProductId" class="text-muted">
        Chọn sản phẩm ở bảng bên dưới hoặc bấm «Thêm sản phẩm».
      </div>

      <template v-else>
        <fieldset :disabled="!canWrite">
        <div class="row mb-3">
          <div class="col-md-4 mb-2">
            <label class="form-label">Tên sản phẩm</label>
            <input v-model="productForm.name" class="form-control" required />
          </div>
          <div class="col-md-3 mb-2">
            <label class="form-label">Danh mục</label>
            <select v-model="productForm.categoryId" class="form-select">
              <option value="">-- Chọn danh mục --</option>
              <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </div>
          <div class="col-md-3 mb-2">
            <label class="form-label">Mô tả</label>
            <input v-model="productForm.description" class="form-control" />
          </div>
          <div class="col-md-2 mb-2 d-flex align-items-end">
            <div class="form-check">
              <input id="productAvailable" v-model="productForm.available" class="form-check-input" type="checkbox" />
              <label class="form-check-label" for="productAvailable">Đang bán</label>
            </div>
          </div>
        </div>
        <div class="mb-3">
          <button type="button" class="btn btn-outline-primary btn-sm" :disabled="savingProduct" @click="saveProduct">
            Lưu thông tin sản phẩm
          </button>
        </div>

        <hr v-if="numericProductId()" />

        <template v-if="numericProductId()">
        <div class="row">
          <div class="col-md-2 mb-2">
            <label class="form-label">Màu</label>
            <input v-model="variantForm.color" class="form-control" />
          </div>
          <div class="col-md-2 mb-2">
            <label class="form-label">Size</label>
            <input v-model="variantForm.size" class="form-control" />
          </div>
          <div class="col-md-3 mb-2">
            <label class="form-label">SKU</label>
            <input v-model="variantForm.sku" class="form-control" required />
          </div>
          <div class="col-md-2 mb-2">
            <label class="form-label">Giá</label>
            <input v-model.number="variantForm.price" type="number" step="0.01" class="form-control" />
          </div>
          <div class="col-md-2 mb-2">
            <label class="form-label">Số lượng</label>
            <input v-model.number="variantForm.quantity" type="number" min="0" class="form-control" />
          </div>
          <div class="col-md-1 mb-2 d-flex align-items-end">
            <div class="form-check">
              <input id="variantDefault" v-model="variantForm.isDefault" class="form-check-input" type="checkbox" />
              <label class="form-check-label" for="variantDefault">Mặc định</label>
            </div>
          </div>
        </div>

        <div class="mb-3">
          <button type="button" class="btn btn-primary btn-sm me-2" :disabled="savingVariant" @click="saveVariant">
            Lưu biến thể
          </button>
          <button type="button" class="btn btn-outline-secondary btn-sm me-2" @click="resetVariantForm">
            Biến thể mới
          </button>
          <button
            v-if="variantForm.id"
            type="button"
            class="btn btn-outline-danger btn-sm"
            @click="deleteVariant"
          >
            Xóa biến thể
          </button>
        </div>

        <div v-if="variantForm.id" class="border rounded p-3 bg-white">
          <h6>Ảnh biến thể</h6>
          <p class="text-muted small mb-2">
            Kéo thả để sắp xếp. Ảnh đầu tiên (sort_order = 1) là ảnh chính của biến thể.
          </p>

          <div class="d-flex flex-wrap gap-2 mb-3">
            <div
              v-for="(img, index) in variantImages"
              :key="img.id"
              class="image-card position-relative"
              draggable="true"
              @dragstart="onDragStart(index)"
              @dragover.prevent
              @drop="onDrop(index)"
            >
              <img :src="img.imageUrl" alt="" />
              <span v-if="index === 0" class="badge bg-primary primary-badge">Chính</span>
              <button type="button" class="btn btn-sm btn-danger delete-btn" @click="removeImage(img.id)">×</button>
            </div>
            <div v-if="!variantImages.length" class="text-muted small">Chưa có ảnh.</div>
          </div>

          <div class="row g-2 align-items-end">
            <div class="col-md-4">
              <label class="form-label">Tải ảnh lên</label>
              <input type="file" class="form-control" accept="image/*" multiple @change="onImageFiles" />
            </div>
            <div class="col-md-5">
              <label class="form-label">Link Google Drive / URL ảnh</label>
              <input v-model="imageUrlInput" class="form-control" placeholder="https://drive.google.com/..." />
            </div>
            <div class="col-md-3">
              <button type="button" class="btn btn-secondary w-100" :disabled="uploading" @click="addImageFromUrl">
                Thêm từ URL
              </button>
            </div>
          </div>
        </div>
        </template>
        </fieldset>
      </template>
    </section>

    <!-- Middle panel: variant list -->
    <section class="border rounded p-3 mb-3">
      <div class="d-flex justify-content-between align-items-center mb-2">
        <h5 class="mb-0">Danh sách biến thể</h5>
        <button
          type="button"
          class="btn btn-sm btn-outline-primary"
          :disabled="!numericProductId()"
          @click="resetVariantForm"
        >
          + Thêm biến thể
        </button>
      </div>
      <table class="table table-sm table-bordered text-center align-middle mb-0">
        <thead class="table-secondary">
          <tr>
            <th>Màu</th>
            <th>Size</th>
            <th>SKU</th>
            <th>Giá</th>
            <th>SL</th>
            <th>Mặc định</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="v in variants"
            :key="v.id"
            :class="{ 'table-active': v.id === variantForm.id }"
            role="button"
            @click="selectVariant(v.id)"
          >
            <td>{{ v.color || "—" }}</td>
            <td>{{ v.size || "—" }}</td>
            <td>{{ v.sku }}</td>
            <td>{{ formatPrice(v.price) }}</td>
            <td>{{ v.quantity ?? 0 }}</td>
            <td>
              <span v-if="v.isDefault" class="badge bg-success">Mặc định</span>
              <span v-else class="text-muted">—</span>
            </td>
          </tr>
          <tr v-if="numericProductId() && !variants.length">
            <td colspan="6" class="text-muted">Sản phẩm chưa có biến thể.</td>
          </tr>
          <tr v-if="!selectedProductId">
            <td colspan="6" class="text-muted">Chưa chọn sản phẩm.</td>
          </tr>
        </tbody>
      </table>
    </section>

    <!-- Bottom panel: product list -->
    <section class="border rounded p-3">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <h5 class="mb-0">Danh sách sản phẩm</h5>
        <button v-if="canWrite" type="button" class="btn btn-sm btn-success" @click="startNewProduct">+ Thêm sản phẩm</button>
      </div>

      <form class="row mb-3 g-2" @submit.prevent="doFilter">
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
          <select v-model.number="pageSize" class="form-select" @change="doFilter">
            <option :value="5">5 / trang</option>
            <option :value="10">10 / trang</option>
            <option :value="20">20 / trang</option>
          </select>
        </div>
        <div class="col-md-1">
          <button type="submit" class="btn btn-dark w-100">Tìm</button>
        </div>
      </form>

      <p v-if="products.length" class="text-muted small mb-2">
        Trang {{ pageInfo.number + 1 }} / {{ pageInfo.totalPages }} · {{ pageInfo.totalElements }} sản phẩm
      </p>

      <nav v-if="showPagination" class="mb-3">
        <ul class="pagination justify-content-center mb-0">
          <li class="page-item" :class="{ disabled: pageInfo.first }">
            <a class="page-link" href="#" @click.prevent="goPage(pageInfo.number - 1)">«</a>
          </li>
          <li
            v-for="i in pageInfo.totalPages"
            :key="i"
            class="page-item"
            :class="{ active: i - 1 === pageInfo.number }"
          >
            <a class="page-link" href="#" @click.prevent="goPage(i - 1)">{{ i }}</a>
          </li>
          <li class="page-item" :class="{ disabled: pageInfo.last }">
            <a class="page-link" href="#" @click.prevent="goPage(pageInfo.number + 1)">»</a>
          </li>
        </ul>
      </nav>

      <table class="table table-bordered text-center align-middle">
        <thead class="table-dark">
          <tr>
            <th v-for="col in productTableColumns" :key="col.key" class="sortable-th">
              <div class="sortable-th__inner">
                <span>{{ col.label }}</span>
                <span class="sort-arrows">
                  <button
                    type="button"
                    class="sort-arrow"
                    :class="{ active: sortBy === col.key && sortDir === 'asc' }"
                    title="Tăng dần"
                    @click.stop="applySort(col.key, 'asc')"
                  >
                    ▲
                  </button>
                  <button
                    type="button"
                    class="sort-arrow"
                    :class="{ active: sortBy === col.key && sortDir === 'desc' }"
                    title="Giảm dần"
                    @click.stop="applySort(col.key, 'desc')"
                  >
                    ▼
                  </button>
                </span>
              </div>
            </th>
            <th>Hành động</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="p in products"
            :key="p.id"
            :class="{ 'table-active': p.id === numericProductId() }"
            role="button"
            @click="selectProduct(p.id)"
          >
            <td>{{ p.id }}</td>
            <td>
              <img v-if="p.image" :src="p.image" alt="" class="product-thumb" />
              <span v-else class="text-muted">—</span>
            </td>
            <td>{{ p.name }}</td>
            <td>{{ formatPrice(p.price) }}</td>
            <td>{{ p.quantity }}</td>
            <td>{{ p.categoryName }}</td>
            <td>{{ formatDate(p.createDate) }}</td>
            <td>
              <span v-if="p.stockStatus === 'OUT'" class="badge bg-danger">Hết hàng</span>
              <span v-else class="badge bg-success">Còn</span>
            </td>
            <td @click.stop>
              <button v-if="canWrite" type="button" class="btn btn-danger btn-sm" @click="removeProduct(p.id)">Xóa</button>
            </td>
          </tr>
          <tr v-if="!products.length">
            <td colspan="9" class="text-muted py-3">Không có sản phẩm.</td>
          </tr>
        </tbody>
      </table>

      <nav v-if="showPagination" class="mt-3">
        <ul class="pagination justify-content-center mb-0">
          <li class="page-item" :class="{ disabled: pageInfo.first }">
            <a class="page-link" href="#" @click.prevent="goPage(pageInfo.number - 1)">«</a>
          </li>
          <li
            v-for="i in pageInfo.totalPages"
            :key="'bottom-' + i"
            class="page-item"
            :class="{ active: i - 1 === pageInfo.number }"
          >
            <a class="page-link" href="#" @click.prevent="goPage(i - 1)">{{ i }}</a>
          </li>
          <li class="page-item" :class="{ disabled: pageInfo.last }">
            <a class="page-link" href="#" @click.prevent="goPage(pageInfo.number + 1)">»</a>
          </li>
        </ul>
      </nav>
    </section>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import AdminLayout from "../../layouts/AdminLayout.vue";
import AdminReadOnlyNotice from "../../components/admin/AdminReadOnlyNotice.vue";
import { apiFetch } from "../../services/http.js";
import { useAppStore } from "../../stores/appStore";
import { userCanWriteCatalog } from "../../utils/adminAccess";

const store = useAppStore();
const canWrite = computed(() => userCanWriteCatalog(store.state.user));

const err = ref("");
const flashOk = ref("");
const flashErr = ref("");
const products = ref([]);
const categories = ref([]);
const variants = ref([]);
const variantImages = ref([]);
const pageInfo = reactive({
  number: 0,
  totalPages: 1,
  totalElements: 0,
  size: 5,
  first: true,
  last: true,
});

const showPagination = computed(
  () => pageInfo.totalPages > 1 || (pageInfo.totalElements > pageInfo.size && !pageInfo.last)
);
const filterKeyword = ref("");
const filterCat = ref("");
const pageSize = ref(5);
const sortBy = ref("id");
const sortDir = ref("desc");

const productTableColumns = [
  { key: "id", label: "ID" },
  { key: "image", label: "Ảnh" },
  { key: "name", label: "Tên" },
  { key: "price", label: "Giá từ" },
  { key: "quantity", label: "Tổng tồn kho" },
  { key: "categoryName", label: "Danh mục" },
  { key: "createDate", label: "Ngày tạo" },
  { key: "stockStatus", label: "Trạng thái" },
];
const selectedProductId = ref(null);
const savingProduct = ref(false);
const savingVariant = ref(false);
const uploading = ref(false);
const imageFiles = ref([]);
const imageUrlInput = ref("");
const dragFromIndex = ref(null);

const productForm = reactive({
  id: null,
  name: "",
  categoryId: "",
  description: "",
  available: true,
});

const variantForm = reactive({
  id: null,
  productId: null,
  color: "",
  size: "",
  sku: "",
  price: 0,
  quantity: 0,
  isDefault: false,
});

function numericProductId() {
  const id = productForm.id ?? selectedProductId.value;
  if (id == null || id === "new") return null;
  return id;
}

function formatPrice(n) {
  return Number(n || 0).toLocaleString("vi-VN");
}

function formatDate(value) {
  if (!value) return "—";
  return new Date(value).toLocaleString("vi-VN");
}

function clearFlash() {
  flashOk.value = "";
  flashErr.value = "";
}

function applyPageMeta(data, requestedPage) {
  const nested = data?.pages && typeof data.pages === "object" && !Array.isArray(data.pages) ? data.pages : {};
  const size = Number(nested.size ?? data.pageSize ?? pageSize.value) || pageSize.value;
  const number = Number(nested.number ?? data.currentPage ?? requestedPage ?? 0);
  let totalElements = Number(nested.totalElements ?? data.totalElements);
  if (!Number.isFinite(totalElements)) {
    totalElements = (data.products || []).length;
  }
  let totalPages = Number(nested.totalPages ?? data.totalPages);
  if (!Number.isFinite(totalPages) || totalPages < 1) {
    totalPages = Math.max(1, Math.ceil(totalElements / size));
  }

  pageInfo.number = number;
  pageInfo.size = size;
  pageInfo.totalElements = totalElements;
  pageInfo.totalPages = totalPages;
  pageInfo.first = nested.first ?? number <= 0;
  pageInfo.last = nested.last ?? number >= totalPages - 1;
}

async function loadProducts(page = 0) {
  err.value = "";
  try {
    const params = new URLSearchParams({
      page: String(page),
      keyword: filterKeyword.value,
      cat: filterCat.value,
      size: String(pageSize.value),
      sortBy: sortBy.value,
      sortDir: sortDir.value,
    });
    const data = await apiFetch(`/api/admin/products?${params}`);
    products.value = data.products || [];
    categories.value = data.categories || [];
    if (data.sortBy) sortBy.value = data.sortBy;
    if (data.sortDir) sortDir.value = data.sortDir;
    applyPageMeta(data, page);
  } catch (e) {
    const msg = String(e?.message || "");
    if (msg.includes("Failed to fetch") || msg.includes("NetworkError") || msg.includes("ECONNREFUSED")) {
      err.value = "Không kết nối được máy chủ. Hãy chạy Spring Boot trên port 8080 rồi tải lại trang.";
    } else {
      err.value = msg || "Lỗi tải danh sách sản phẩm";
    }
    throw e;
  }
}

async function loadProductDetail(productId) {
  const data = await apiFetch(`/api/admin/products/${productId}`);
  const p = data.product || {};
  productForm.id = p.id;
  productForm.name = p.name || "";
  productForm.categoryId = p.categoryId || "";
  productForm.description = p.description || "";
  productForm.available = p.available !== false;
}

async function loadVariants(productId) {
  const data = await apiFetch(`/api/admin/products/${productId}/variants`);
  variants.value = data.variants || [];
}

async function loadVariantDetail(variantId) {
  const v = await apiFetch(`/api/admin/products/variants/${variantId}`);
  variantForm.id = v.id;
  variantForm.productId = v.productId;
  variantForm.color = v.color || "";
  variantForm.size = v.size || "";
  variantForm.sku = v.sku || "";
  variantForm.price = v.price ?? 0;
  variantForm.quantity = v.quantity ?? 0;
  variantForm.isDefault = !!v.isDefault;
  variantImages.value = v.images || [];
}

function resetVariantForm() {
  const pid = numericProductId();
  if (!pid) return;
  variantForm.id = null;
  variantForm.productId = pid;
  variantForm.color = "";
  variantForm.size = "";
  variantForm.sku = "";
  variantForm.price = 0;
  variantForm.quantity = 0;
  variantForm.isDefault = variants.value.length === 0;
  variantImages.value = [];
}

async function selectProduct(productId) {
  clearFlash();
  selectedProductId.value = productId;
  productForm.id = productId;
  try {
    await loadProductDetail(productId);
    await loadVariants(productId);
    const defaultVariant = variants.value.find((v) => v.isDefault) || variants.value[0];
    if (defaultVariant) {
      await loadVariantDetail(defaultVariant.id);
    } else {
      resetVariantForm();
    }
  } catch (e) {
    err.value = e.message || "Lỗi tải sản phẩm";
  }
}

async function selectVariant(variantId) {
  clearFlash();
  try {
    await loadVariantDetail(variantId);
  } catch (e) {
    flashErr.value = e.message || "Lỗi tải biến thể";
  }
}

function startNewProduct() {
  clearFlash();
  selectedProductId.value = "new";
  productForm.id = null;
  productForm.name = "";
  productForm.categoryId = categories.value[0]?.id || "";
  productForm.description = "";
  productForm.available = true;
  variants.value = [];
  variantForm.id = null;
  variantForm.productId = null;
  variantImages.value = [];
}

async function saveProduct() {
  clearFlash();
  savingProduct.value = true;
  const params = new URLSearchParams();
  if (productForm.id && productForm.id !== "new") params.set("id", String(productForm.id));
  params.set("name", productForm.name);
  if (productForm.categoryId) params.set("categoryId", productForm.categoryId);
  params.set("description", productForm.description || "");
  params.set("available", productForm.available ? "true" : "false");

  try {
    const data = await apiFetch(`/api/admin/products/save?${params}`, { method: "POST" });
    flashOk.value = data.message || "Lưu thành công!";
    const productId = data.productId;
    selectedProductId.value = productId;
    productForm.id = productId;
    await loadProducts(pageInfo.number);
    await loadProductDetail(productId);
    await loadVariants(productId);
    const defaultVariant = variants.value.find((v) => v.isDefault) || variants.value[0];
    if (defaultVariant) {
      await loadVariantDetail(defaultVariant.id);
    } else {
      resetVariantForm();
    }
  } catch (e) {
    flashErr.value = e.message || "Lưu sản phẩm thất bại";
  } finally {
    savingProduct.value = false;
  }
}

async function saveVariant() {
  const productId = numericProductId();
  if (!productId) {
    flashErr.value = "Lưu thông tin sản phẩm trước khi thêm biến thể.";
    return;
  }
  clearFlash();
  savingVariant.value = true;
  try {
    const payload = {
      id: variantForm.id,
      productId,
      sku: variantForm.sku,
      size: variantForm.size,
      color: variantForm.color,
      price: variantForm.price,
      quantity: variantForm.quantity,
      isDefault: variantForm.isDefault,
      status: true,
    };
    const data = await apiFetch("/api/admin/products/variants/save", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    flashOk.value = data.message || "Lưu biến thể thành công!";
    await loadVariants(productId);
    await loadProducts(pageInfo.number);
    if (data.variant?.id) {
      await loadVariantDetail(data.variant.id);
    }
  } catch (e) {
    flashErr.value = e.message || "Lưu biến thể thất bại";
  } finally {
    savingVariant.value = false;
  }
} 

function onImageFiles(e) {
  imageFiles.value = Array.from(e.target.files || []);
  if (imageFiles.value.length) {
    uploadImages();
  }
}

async function uploadImages() {
  if (!variantForm.id || !imageFiles.value.length) return;
  clearFlash();
  uploading.value = true;
  const fd = new FormData();
  imageFiles.value.forEach((f) => fd.append("files", f));
  try {
    const data = await apiFetch(`/api/admin/products/variants/${variantForm.id}/images`, {
      method: "POST",
      body: fd,
    });
    flashOk.value = data.message || "Tải ảnh thành công!";
    await loadVariantDetail(variantForm.id);
    await loadProducts(pageInfo.number);
    imageFiles.value = [];
  } catch (e) {
    flashErr.value = e.message || "Tải ảnh thất bại";
  } finally {
    uploading.value = false;
  }
}

async function addImageFromUrl() {
  if (!variantForm.id || !imageUrlInput.value.trim()) return;
  clearFlash();
  uploading.value = true;
  try {
    const data = await apiFetch(`/api/admin/products/variants/${variantForm.id}/images/url`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ url: imageUrlInput.value.trim() }),
    });
    flashOk.value = data.message || "Thêm ảnh thành công!";
    imageUrlInput.value = "";
    await loadVariantDetail(variantForm.id);
    await loadProducts(pageInfo.number);
  } catch (e) {
    flashErr.value = e.message || "Thêm ảnh thất bại";
  } finally {
    uploading.value = false;
  }
}

async function removeImage(imageId) {
  if (!confirm("Xóa ảnh này?")) return;
  clearFlash();
  try {
    await apiFetch(`/api/admin/products/variants/images/${imageId}`, { method: "DELETE" });
    flashOk.value = "Đã xóa ảnh";
    await loadVariantDetail(variantForm.id);
    await loadProducts(pageInfo.number);
  } catch (e) {
    flashErr.value = e.message || "Xóa ảnh thất bại";
  }
}

function onDragStart(index) {
  dragFromIndex.value = index;
}

async function onDrop(toIndex) {
  const fromIndex = dragFromIndex.value;
  dragFromIndex.value = null;
  if (fromIndex === null || fromIndex === toIndex) return;

  const reordered = [...variantImages.value];
  const [moved] = reordered.splice(fromIndex, 1);
  reordered.splice(toIndex, 0, moved);
  variantImages.value = reordered;

  try {
    await apiFetch(`/api/admin/products/variants/${variantForm.id}/images/reorder`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ imageIds: reordered.map((img) => img.id) }),
    });
    await loadVariantDetail(variantForm.id);
    await loadProducts(pageInfo.number);
  } catch (e) {
    flashErr.value = e.message || "Sắp xếp ảnh thất bại";
    await loadVariantDetail(variantForm.id);
  }
}

async function deleteVariant() {
  if (!variantForm.id || !confirm("Xóa biến thể này?")) return;
  clearFlash();
  try { 
    await apiFetch(`/api/admin/products/variants/${variantForm.id}`, { method: "DELETE" });
    flashOk.value = "Đã xóa biến thể";
    await loadVariants(numericProductId());
    await loadProducts(pageInfo.number);
    const next = variants.value.find((v) => v.isDefault) || variants.value[0];
    if (next) {
      await loadVariantDetail(next.id);
    } else {
      resetVariantForm();
    }
  } catch (e) {
    flashErr.value = e.message || "Xóa biến thể thất bại";
  }
}

async function removeProduct(id) {
  if (!confirm("Xóa sản phẩm?")) return;
  clearFlash();
  try {
    const res = await apiFetch(`/api/admin/products/${id}`, { method: "DELETE" });
    showFlashOk(res.message || "Đã xóa");
    if (selectedProductId.value === id) {
      selectedProductId.value = null;
      variants.value = [];
      variantImages.value = [];
    }
    await loadProducts(pageInfo.number);
  } catch (e) {
    flashErr.value = e.message || "Xóa thất bại";
  }
}

function doFilter() {
  loadProducts(0).catch((e) => {
    err.value = e.message || "Lỗi";
  });
}

function applySort(column, direction) {
  sortBy.value = column;
  sortDir.value = direction;
  loadProducts(0).catch((e) => {
    err.value = e.message || "Lỗi";
  });
}

function goPage(p) {
  if (p < 0 || p >= pageInfo.totalPages) return;
  loadProducts(p).catch((e) => {
    err.value = e.message || "Lỗi";
  });
}

function showFlashOk(message) {
  flashOk.value = message;

  setTimeout(() => {
    clearFlash();
  }, 3000);  
}

function showFlashErr(message) {
  flashErr.value = message;

  setTimeout(() => {
    clearFlash();
  }, 3000);  
}

onMounted(() => {
  loadProducts(0).catch((e) => {
    err.value = e.message || "Lỗi tải";
  });
});
</script>

<style scoped>
.product-thumb {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 4px;
}

.image-card {
  width: 110px;
  height: 110px;
  border: 1px solid #dee2e6;
  border-radius: 6px;
  overflow: hidden;
  cursor: grab;
  background: #fff;
}

.image-card img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.primary-badge {
  position: absolute;
  top: 4px;
  left: 4px;
}

.delete-btn {
  position: absolute;
  top: 2px;
  right: 2px;
  padding: 0 6px;
  line-height: 1.2;
}

.sortable-th {
  white-space: nowrap;
  vertical-align: middle;
}

.sortable-th__inner {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
}

.sort-arrows {
  display: inline-flex;
  flex-direction: column;
  line-height: 0.85;
}

.sort-arrow {
  border: 0;
  background: transparent;
  color: rgba(255, 255, 255, 0.45);
  font-size: 0.65rem;
  padding: 0;
  line-height: 1;
  cursor: pointer;
}

.sort-arrow:hover {
  color: rgba(255, 255, 255, 0.85);
}

.sort-arrow.active {
  color: #ffc107;
}
</style>
