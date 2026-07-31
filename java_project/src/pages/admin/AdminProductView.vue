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
            <label class="form-label">Tên sản phẩm <span class="text-danger">*</span></label>
            <input
              v-model="productForm.name"
              class="form-control"
              :class="{ 'is-invalid': productErrors.name }"
              @input="clearProductError('name')"
            />
            <div v-if="productErrors.name" class="invalid-feedback d-block">{{ productErrors.name }}</div>
          </div>
          <div class="col-md-3 mb-2">
            <label class="form-label">Danh mục <span class="text-danger">*</span></label>
            <select
              v-model="productForm.categoryId"
              class="form-select"
              :class="{ 'is-invalid': productErrors.categoryId }"
              @change="clearProductError('categoryId')"
            >
              <option value="">-- Chọn danh mục --</option>
              <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
            <div v-if="productErrors.categoryId" class="invalid-feedback d-block">{{ productErrors.categoryId }}</div>
          </div>
          <div class="col-md-3 mb-2">
            <label class="form-label">Mô tả</label>
            <input
              v-model="productForm.description"
              class="form-control"
              :class="{ 'is-invalid': productErrors.description }"
              maxlength="500"
              @input="clearProductError('description')"
            />
            <div v-if="productErrors.description" class="invalid-feedback d-block">{{ productErrors.description }}</div>
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
            <label class="form-label">Màu <span class="text-danger">*</span></label>
            <div class="input-group">
              <span
                class="input-group-text color-swatch"
                :title="variantForm.color || 'Chưa chọn'"
                :style="{ background: colorSwatchHex(variantForm.color) }"
              ></span>
              <input
                v-model="variantForm.color"
                class="form-control"
                :class="{ 'is-invalid': variantErrors.color }"
                list="admin-color-suggestions"
                placeholder="Vd: Đen"
                @change="suggestSkuFromColorSize"
                @input="clearVariantError('color')"
              />
            </div>
            <datalist id="admin-color-suggestions">
              <option v-for="c in colorSuggestions" :key="c" :value="c" />
            </datalist>
            <div v-if="variantErrors.color" class="invalid-feedback d-block">{{ variantErrors.color }}</div>
          </div>
          <div class="col-md-2 mb-2">
            <label class="form-label">Size <span class="text-danger">*</span></label>
            <input
              v-model="variantForm.size"
              class="form-control"
              :class="{ 'is-invalid': variantErrors.size }"
              list="admin-size-suggestions"
              placeholder="Vd: 39 / M"
              @change="suggestSkuFromColorSize"
              @input="clearVariantError('size')"
            />
            <datalist id="admin-size-suggestions">
              <option v-for="s in sizeSuggestions" :key="s" :value="s" />
            </datalist>
            <div v-if="variantErrors.size" class="invalid-feedback d-block">{{ variantErrors.size }}</div>
          </div>
          <div class="col-md-3 mb-2">
            <label class="form-label">SKU <span class="text-danger">*</span></label>
            <input
              v-model="variantForm.sku"
              class="form-control"
              :class="{ 'is-invalid': variantErrors.sku }"
              @input="clearVariantError('sku')"
            />
            <div v-if="variantErrors.sku" class="invalid-feedback d-block">{{ variantErrors.sku }}</div>
          </div>
          <div class="col-md-2 mb-2">
            <label class="form-label">Giá <span class="text-danger">*</span></label>
            <input
              v-model.number="variantForm.price"
              type="number"
              step="0.01"
              min="0"
              class="form-control"
              :class="{ 'is-invalid': variantErrors.price }"
              @input="clearVariantError('price')"
            />
            <div v-if="variantErrors.price" class="invalid-feedback d-block">{{ variantErrors.price }}</div>
          </div>
          <div class="col-md-2 mb-2">
            <label class="form-label">Số lượng <span class="text-danger">*</span></label>
            <input
              v-model.number="variantForm.quantity"
              type="number"
              min="0"
              max="9999"
              step="1"
              class="form-control"
              :class="{ 'is-invalid': variantErrors.quantity }"
              placeholder="Nhập tay (0–9999)"
              @input="clearVariantError('quantity')"
            />
            <div v-if="variantErrors.quantity" class="invalid-feedback d-block">{{ variantErrors.quantity }}</div>
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
          <h6>Ảnh biến thể (4 góc)</h6>

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
              <img :src="img.imageUrl" :alt="`Góc ${index + 1}`" />
              <span class="badge bg-dark primary-badge">Góc {{ index + 1 }}</span>
              <button type="button" class="btn btn-sm btn-danger delete-btn" @click="removeImage(img.id)">×</button>
            </div>
            <div v-if="!variantImages.length" class="text-muted small">Chưa có ảnh — tải tối đa 4 góc.</div>
          </div>

          <div class="row g-2 align-items-end">
            <div class="col-md-4">
              <label class="form-label">Thêm ảnh (không xóa ảnh cũ)</label>
              <input type="file" class="form-control" accept="image/*" multiple @change="onImageFiles($event, false)" />
            </div>
            <div class="col-md-4">
              <label class="form-label">Thay thế 4 góc (xóa cũ + tên chuẩn)</label>
              <input type="file" class="form-control" accept="image/*" multiple @change="onImageFiles($event, true)" />
            </div>
            <div class="col-md-4">
              <label class="form-label">Link Google Drive / URL ảnh</label>
              <div class="d-flex gap-2">
                <input v-model="imageUrlInput" class="form-control" placeholder="https://drive.google.com/..." />
                <button type="button" class="btn btn-secondary" :disabled="uploading" @click="addImageFromUrl">
                  Thêm
                </button>
              </div>
            </div>
          </div>
          <div class="mt-2">
            <button
              type="button"
              class="btn btn-outline-primary btn-sm"
              :disabled="uploading || !variantImages.length"
              @click="resyncImageNames"
            >
              Đồng bộ lại tên ảnh Cloudinary
            </button>
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
            <th>Ảnh</th>
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
            <td>
              <img
                v-if="variantThumb(v)"
                :src="variantThumb(v)"
                class="variant-list-thumb"
                :alt="v.color || v.sku"
              />
              <span v-else class="text-muted">—</span>
            </td>
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
            <td colspan="7" class="text-muted">Sản phẩm chưa có biến thể.</td>
          </tr>
          <tr v-if="!selectedProductId">
            <td colspan="7" class="text-muted">Chưa chọn sản phẩm.</td>
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
import { firstError, getApiError, runValidation } from "../../utils/validators";

const store = useAppStore();
const canWrite = computed(() => userCanWriteCatalog(store.state.user));

const err = ref("");
const flashOk = ref("");
const flashErr = ref("");
const productErrors = reactive({});
const variantErrors = reactive({});
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
  price: null,
  quantity: null,
  isDefault: false,
});

const colorSuggestions = ref([
  "Đen",
  "Trắng",
  "Xám",
  "Xám đậm",
  "Đỏ",
  "Hồng",
  "Cam",
  "Vàng",
  "Be",
  "Nâu",
  "Xanh Navy",
  "Xanh Dương",
  "Xanh Lá",
  "Tím",
  "Bạc",
]);
const sizeSuggestions = ["39", "40", "41", "42", "43", "S", "M", "L", "XL", "XXL", "Free"];

const COLOR_HEX = {
  đen: "#1a1a1a",
  trắng: "#f5f5f5",
  xám: "#8c8c8c",
  "xám đậm": "#505050",
  đỏ: "#c82828",
  hồng: "#e678a0",
  cam: "#e67828",
  vàng: "#e6c832",
  be: "#d2be96",
  nâu: "#784b2d",
  "xanh navy": "#192d5f",
  "xanh dương": "#2864c8",
  "xanh lá": "#2d8c46",
  tím: "#783ca0",
  bạc: "#c0c0c0",
};

function colorSwatchHex(name) {
  const key = String(name || "")
    .trim()
    .toLowerCase();
  return COLOR_HEX[key] || "#dee2e6";
}

function skuSlug(value) {
  return String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, "")
    .slice(0, 12);
}

function suggestSkuFromColorSize() {
  const pid = numericProductId();
  if (!pid) return;
  const color = String(variantForm.color || "").trim();
  const size = String(variantForm.size || "").trim();
  if (!color && !size) return;
  const current = String(variantForm.sku || "").trim();
  // Chỉ tự điền khi SKU trống hoặc đang theo pattern SKU-{id}-*
  if (current && !/^SKU-\d+-/i.test(current)) return;
  const parts = [`SKU-${pid}`];
  if (color) parts.push(skuSlug(color) || "COLOR");
  if (size) parts.push(skuSlug(size) || "SIZE");
  variantForm.sku = parts.join("-");
}

async function loadColorSuggestions() {
  try {
    const data = await apiFetch("/api/admin/products/color-suggestions");
    if (Array.isArray(data.colors) && data.colors.length) {
      colorSuggestions.value = data.colors;
    }
  } catch {
    /* giữ list mặc định */
  }
}

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

function clearProductError(key) {
  delete productErrors[key];
}

function clearVariantError(key) {
  delete variantErrors[key];
}

function clearProductErrors() {
  Object.keys(productErrors).forEach((k) => delete productErrors[k]);
}

function clearVariantErrors() {
  Object.keys(variantErrors).forEach((k) => delete variantErrors[k]);
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
  clearVariantErrors();
  variantForm.id = null;
  variantForm.productId = pid;
  variantForm.color = "";
  variantForm.size = "";
  variantForm.sku = "";
  variantForm.price = null;
  variantForm.quantity = null; // bắt buộc nhập tay — không tự tăng/tự điền
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
  clearProductErrors();
  const result = runValidation(
    {
      name: productForm.name,
      categoryId: productForm.categoryId,
      description: productForm.description,
    },
    {
      name: [
        "required",
        { type: "min", min: 2, message: "Tên sản phẩm tối thiểu 2 ký tự." },
        { type: "max", max: 200, message: "Tên sản phẩm tối đa 200 ký tự." },
      ],
      categoryId: [{ type: "required", message: "Vui lòng chọn danh mục." }],
      description: [{ type: "max", max: 500, message: "Mô tả tối đa 500 ký tự." }],
    }
  );
  if (!result.ok) {
    Object.assign(productErrors, result.errors);
    flashErr.value = firstError(result.errors);
    return;
  }

  savingProduct.value = true;
  const params = new URLSearchParams();
  if (productForm.id && productForm.id !== "new") params.set("id", String(productForm.id));
  params.set("name", String(result.values.name));
  params.set("categoryId", String(result.values.categoryId));
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
    const api = getApiError(e, "Lưu sản phẩm thất bại");
    Object.assign(productErrors, api.errors || {});
    flashErr.value = api.message;
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
  clearVariantErrors();

  const isPlaceholder = (v) => {
    const s = String(v || "").trim().toLowerCase();
    return !s || s === "default" || s === "mặc định" || s === "mac dinh";
  };

  const result = runValidation(
    {
      color: variantForm.color,
      size: variantForm.size,
      sku: variantForm.sku,
      price: variantForm.price,
      quantity: variantForm.quantity,
    },
    {
      color: [
        { type: "required", message: "Màu sắc là bắt buộc." },
        { type: "max", max: 50, message: "Màu tối đa 50 ký tự." },
      ],
      size: [
        { type: "required", message: "Kích cỡ là bắt buộc." },
        { type: "max", max: 50, message: "Size tối đa 50 ký tự." },
      ],
      sku: [
        "required",
        { type: "min", min: 2, message: "SKU tối thiểu 2 ký tự." },
        { type: "max", max: 100, message: "SKU tối đa 100 ký tự." },
      ],
      price: [
        { type: "required", message: "Giá là bắt buộc." },
        { type: "number", gt: 0, message: "Giá phải lớn hơn 0." },
      ],
      quantity: [
        { type: "required", message: "Số lượng là bắt buộc (nhập tay, không tự tăng)." },
        { type: "number", min: 0, max: 9999, message: "Số lượng từ 0 đến 9999." },
      ],
    }
  );

  if (!result.ok) {
    Object.assign(variantErrors, result.errors);
    flashErr.value = firstError(result.errors);
    return;
  }

  if (isPlaceholder(result.values.color)) {
    variantErrors.color = "Màu không được là mặc định/trống.";
    flashErr.value = variantErrors.color;
    return;
  }
  if (isPlaceholder(result.values.size)) {
    variantErrors.size = "Size không được là mặc định/trống.";
    flashErr.value = variantErrors.size;
    return;
  }

  const size = String(result.values.size).trim();
  const color = String(result.values.color).trim();
  const dup = variants.value.find(
    (v) =>
      v.id !== variantForm.id &&
      String(v.size || "").trim().toLowerCase() === size.toLowerCase() &&
      String(v.color || "").trim().toLowerCase() === color.toLowerCase()
  );
  if (dup) {
    variantErrors.color = "Đã tồn tại biến thể với màu và size này.";
    variantErrors.size = variantErrors.color;
    flashErr.value = variantErrors.color;
    return;
  }

  // Số lượng phải là số nguyên (không tự tăng)
  const quantity = Number(result.values.quantity);
  if (!Number.isInteger(quantity)) {
    variantErrors.quantity = "Số lượng phải là số nguyên.";
    flashErr.value = variantErrors.quantity;
    return;
  }

  savingVariant.value = true;
  try {
    const payload = {
      id: variantForm.id,
      productId,
      sku: String(result.values.sku).trim(),
      size,
      color,
      price: Number(result.values.price),
      quantity,
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
    const api = getApiError(e, "Lưu biến thể thất bại");
    Object.assign(variantErrors, api.errors || {});
    flashErr.value = api.message;
  } finally {
    savingVariant.value = false;
  }
} 

async function uploadImages(replace = false) {
  if (!variantForm.id || !imageFiles.value.length) return;
  clearFlash();
  uploading.value = true;
  try {
    const fd = new FormData();
    // Chỉ lấy tối đa 4 file khi thay thế
    const files = replace ? imageFiles.value.slice(0, 4) : imageFiles.value;
    files.forEach((f) => fd.append("files", f));
    const qs = replace ? "?replace=true" : "";
    const data = await apiFetch(`/api/admin/products/variants/${variantForm.id}/images${qs}`, {
      method: "POST",
      body: fd,
    });
    flashOk.value = data.message || "Tải ảnh thành công!";
    if (data.detectedColor && (!variantForm.color || isPlaceholderColor(variantForm.color))) {
      variantForm.color = data.detectedColor;
      suggestSkuFromColorSize();
    }
    if (data.variant) {
      variantForm.color = data.variant.color || variantForm.color;
      variantForm.size = data.variant.size || variantForm.size;
      variantForm.sku = data.variant.sku || variantForm.sku;
    }
    imageFiles.value = [];
    await loadVariantDetail(variantForm.id);
    await loadVariants(numericProductId());
    await loadProducts(pageInfo.number);
  } catch (e) {
    flashErr.value = e.message || "Tải ảnh thất bại";
  } finally {
    uploading.value = false;
  }
}

function variantThumb(v) {
  const imgs = v?.images;
  if (Array.isArray(imgs) && imgs.length && imgs[0]?.imageUrl) {
    return imgs[0].imageUrl;
  }
  return null;
}

function isPlaceholderColor(v) {
  const s = String(v || "").trim().toLowerCase();
  return !s || s === "default" || s === "mặc định" || s === "mac dinh";
}

/** Đọc màu từ file ảnh trên trình duyệt (trước khi upload) để điền form. */
async function detectColorFromLocalFile(file) {
  if (!file || !file.type?.startsWith("image/")) return null;
  // Ưu tiên tên file có chứa màu
  const nameHint = detectColorFromFilename(file.name);
  if (nameHint) return nameHint;

  return new Promise((resolve) => {
    const url = URL.createObjectURL(file);
    const img = new Image();
    img.onload = () => {
      try {
        const canvas = document.createElement("canvas");
        const size = 64;
        canvas.width = size;
        canvas.height = size;
        const ctx = canvas.getContext("2d", { willReadFrequently: true });
        ctx.drawImage(img, 0, 0, size, size);
        const data = ctx.getImageData(0, 0, size, size).data;
        let r = 0,
          g = 0,
          b = 0,
          n = 0;
        for (let i = 0; i < data.length; i += 16) {
          const rr = data[i];
          const gg = data[i + 1];
          const bb = data[i + 2];
          const aa = data[i + 3];
          if (aa < 128) continue;
          const max = Math.max(rr, gg, bb);
          const min = Math.min(rr, gg, bb);
          if (max > 245 && min > 230) continue;
          if (max < 20) continue;
          r += rr;
          g += gg;
          b += bb;
          n++;
        }
        URL.revokeObjectURL(url);
        if (!n) {
          resolve("Đen");
          return;
        }
        resolve(nearestColorName(Math.round(r / n), Math.round(g / n), Math.round(b / n)));
      } catch {
        URL.revokeObjectURL(url);
        resolve(null);
      }
    };
    img.onerror = () => {
      URL.revokeObjectURL(url);
      resolve(null);
    };
    img.src = url;
  });
}

function detectColorFromFilename(filename) {
  const base = String(filename || "").toLowerCase();
  const rules = [
    [/đen|den|black|blk/, "Đen"],
    [/trắng|trang|white|wht/, "Trắng"],
    [/xám\s*đậm|xam\s*dam|charcoal/, "Xám đậm"],
    [/xám|xam|gr[ae]y/, "Xám"],
    [/đỏ|(^|[^a-z])do([^a-z]|$)|red/, "Đỏ"],
    [/hồng|hong|pink/, "Hồng"],
    [/cam|orange/, "Cam"],
    [/vàng|vang|yellow|gold/, "Vàng"],
    [/beige|cream|(^|[^a-z])be([^a-z]|$)/, "Be"],
    [/nâu|nau|brown/, "Nâu"],
    [/navy|xanh\s*navy/, "Xanh Navy"],
    [/xanh\s*dương|blue/, "Xanh Dương"],
    [/xanh\s*lá|xanh\s*la|green/, "Xanh Lá"],
    [/tím|tim|purple|violet/, "Tím"],
    [/bạc|bac|silver/, "Bạc"],
  ];
  for (const [re, name] of rules) {
    if (re.test(base)) return name;
  }
  return null;
}

function nearestColorName(r, g, b) {
  const palette = [
    ["Đen", 25, 25, 25],
    ["Trắng", 245, 245, 245],
    ["Xám", 140, 140, 140],
    ["Xám đậm", 80, 80, 80],
    ["Đỏ", 200, 40, 40],
    ["Hồng", 230, 120, 160],
    ["Cam", 230, 120, 40],
    ["Vàng", 230, 200, 50],
    ["Be", 210, 190, 150],
    ["Nâu", 120, 75, 45],
    ["Xanh Navy", 25, 45, 95],
    ["Xanh Dương", 40, 100, 200],
    ["Xanh Lá", 45, 140, 70],
    ["Tím", 120, 60, 160],
    ["Bạc", 192, 192, 192],
  ];
  let best = palette[0][0];
  let bestD = Infinity;
  for (const [name, pr, pg, pb] of palette) {
    const d = (r - pr) ** 2 + (g - pg) ** 2 + (b - pb) ** 2;
    if (d < bestD) {
      bestD = d;
      best = name;
    }
  }
  return best;
}

function onImageFiles(e, replace = false) {
  imageFiles.value = Array.from(e.target.files || []);
  if (!imageFiles.value.length) return;
  // Tự điền màu trên form trước khi/ trong lúc upload
  detectColorFromLocalFile(imageFiles.value[0]).then((color) => {
    if (color && isPlaceholderColor(variantForm.color)) {
      variantForm.color = color;
      suggestSkuFromColorSize();
    }
  });
  uploadImages(replace);
  e.target.value = "";
}

async function resyncImageNames() {
  if (!variantForm.id) return;
  if (!confirm("Đồng bộ lại tên Cloudinary theo chuẩn sp{id}_{biến_thể}_{1-4}?")) return;
  clearFlash();
  uploading.value = true;
  try {
    const data = await apiFetch(`/api/admin/products/variants/${variantForm.id}/images/resync`, {
      method: "POST",
    });
    flashOk.value = data.message || "Đã đồng bộ tên ảnh!";
    variantImages.value = data.images || [];
    await loadVariantDetail(variantForm.id);
  } catch (e) {
    flashErr.value = e.message || "Đồng bộ tên ảnh thất bại";
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
    await loadVariants(numericProductId());
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
  loadColorSuggestions();
  loadProducts(0).catch((e) => {
    err.value = e.message || "Lỗi tải";
  });
});
</script>

<style scoped>
.color-swatch {
  width: 2.25rem;
  padding: 0;
  border: 1px solid #ced4da;
}

.product-thumb {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 4px;
}

.variant-list-thumb {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid #dee2e6;
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
