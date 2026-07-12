<template>
  <MainLayout>
    <template #full>
      <div class="page-hero page-hero--products">
        <img
          src="https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=1600&q=80"
          alt="Bộ sưu tập giày thể thao Sports4Life"
        />
        <div class="page-hero__content site-container">
          <p class="page-hero__eyebrow">New Collection 2026</p>
          <h1>Sản phẩm</h1>
          <p>Giày &amp; trang phục thể thao chính hãng — phong cách năng động, sẵn sàng cho mọi trận đấu</p>
          <div class="product-poster__tags">
            <span>Chính hãng</span>
            <span>Miễn phí đổi trả 30 ngày</span>
            <span>Giao nhanh 2–4 ngày</span>
          </div>
        </div>
      </div>
    </template>

    <div v-if="message" class="alert alert-success py-2">{{ message }}</div>
    <div v-if="error" class="alert alert-danger py-2">{{ error }}</div>

    <div class="product-page">
      <aside class="product-sidebar">
        <div class="product-category">
          <h3>
            <span class="product-category__icon">☰</span>
            Danh mục
          </h3>
          <a
            href="#"
            class="product-category__link"
            :class="{ active: !filters.cat }"
            @click.prevent="selectCategory('')"
          >
            Tất cả sản phẩm
          </a>
          <a
            v-for="cat in categories"
            :key="cat.id || cat.name"
            href="#"
            class="product-category__link"
            :class="{ active: filters.cat === cat.name || filters.cat === cat.id }"
            @click.prevent="selectCategory(cat.name)"
          >
            <span>{{ categoryIcon(cat.name) }}</span>
            {{ cat.name }}
          </a>
        </div>

        <div class="product-price-filter">
          <h3>
            <span class="product-category__icon" aria-hidden="true">₫</span>
            Khoảng giá
          </h3>

          <ul class="product-price-filter__list" role="list">
            <li v-for="range in priceRanges" :key="range.label">
              <button
                type="button"
                class="product-price-filter__chip"
                :class="{ active: isPriceRangeActive(range) }"
                :aria-pressed="isPriceRangeActive(range)"
                @click="selectPriceRange(range)"
              >
                <span class="product-price-filter__radio" aria-hidden="true" />
                <span class="product-price-filter__chip-text">{{ range.label }}</span>
              </button>
            </li>
          </ul>

          <div class="product-price-filter__divider">
            <span>Hoặc nhập khoảng</span>
          </div>

          <div class="product-price-filter__custom">
            <label class="product-price-filter__input-wrap">
              <span>Từ</span>
              <input
                v-model.number="filters.min"
                type="number"
                min="0"
                step="1000"
                placeholder="0"
                :class="{ 'is-invalid': fieldErrors.min }"
                @input="clearFieldError('min')"
              />
            </label>
            <span class="product-price-filter__dash" aria-hidden="true">–</span>
            <label class="product-price-filter__input-wrap">
              <span>Đến</span>
              <input
                v-model.number="filters.max"
                type="number"
                min="0"
                step="1000"
                placeholder="∞"
                :class="{ 'is-invalid': fieldErrors.max }"
                @input="clearFieldError('max')"
              />
            </label>
          </div>
          <p v-if="fieldErrors.min || fieldErrors.max" class="product-filter__error">
            {{ fieldErrors.min || fieldErrors.max }}
          </p>
          <div class="product-price-filter__actions">
            <button type="button" class="product-price-filter__apply" :disabled="loading" @click="onFilter">
              Áp dụng
            </button>
            <button
              v-if="hasPriceFilter"
              type="button"
              class="product-price-filter__clear"
              :disabled="loading"
              @click="clearPriceFilter"
            >
              Xóa lọc
            </button>
          </div>
        </div>

        <div class="product-sidebar__promo">
          <p class="product-sidebar__promo-tag">Ưu đãi hot</p>
          <h4>Giảm đến 20%</h4>
          <p>Cho đơn hàng đầu tiên &amp; sản phẩm sale cuối tuần</p>
          <RouterLink to="/featured" class="product-sidebar__promo-btn">Xem deal →</RouterLink>
        </div>

        <div class="product-sidebar__help">
          <span>💬</span>
          <div>
            <strong>Cần tư vấn size?</strong>
            <p>Chat với đội ngũ Sports4Life 8h–22h mỗi ngày.</p>
          </div>
        </div>
      </aside>

      <section class="product-list">
        <div class="product-toolbar">
          <div>
            <h2 class="product-toolbar__title">Danh sách sản phẩm</h2>
            <p class="product-toolbar__meta">
              <span v-if="loading">Đang tải...</span>
              <span v-else>{{ totalLabel }}</span>
              <span v-if="filters.cat" class="product-toolbar__chip">{{ filters.cat }}</span>
              <span v-if="priceFilterLabel" class="product-toolbar__chip">{{ priceFilterLabel }}</span>
            </p>
          </div>
        </div>

        <form class="product-filter" @submit.prevent="onFilter">
          <div class="product-filter__field product-filter__field--search">
            <div class="product-filter__search" :class="{ 'is-invalid': fieldErrors.keyword }">
              <span aria-hidden="true">🔍</span>
              <input
                v-model="filters.keyword"
                type="text"
                maxlength="100"
                placeholder="Tìm tên sản phẩm, thương hiệu..."
                @input="clearFieldError('keyword')"
              />
            </div>
            <p v-if="fieldErrors.keyword" class="product-filter__error">{{ fieldErrors.keyword }}</p>
          </div>

          <button type="submit" class="product-filter__btn" :disabled="loading">
            {{ loading ? "..." : "Tìm" }}
          </button>
        </form>

        <div v-if="loading" class="product-loading">
          <div v-for="n in 6" :key="n" class="product-skeleton" />
        </div>
        <ProductList
          v-else
          :products="products"
          :sort="filters.sort"
          :dir="filters.dir"
          @change-page="changePage"
          @add-to-cart="addToCart"
          @sort-change="setSort"
        />
      </section>
    </div>

    <BrandStrip />
  </MainLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import MainLayout from "../layouts/MainLayout.vue";
import ProductList from "../components/ProductList.vue";
import BrandStrip from "../components/BrandStrip.vue";
import { fetchProductsApi } from "../services/api";
import { useAppStore, useToast } from "../stores/appStore";
import {
  applyRouteQueryToFilters,
  buildProductQueryParams,
  filtersToRouteQuery,
  validateProductFilters,
} from "../utils/productFilter";

const categoryIcons = {
  "Giày chạy bộ": "👟",
  "Giày đá bóng": "⚽",
  "Giày tập gym": "🏋️",
  "Áo thể thao": "👕",
  "Quần thể thao": "🩳",
  "Áo khoác thể thao": "🧥",
  "Bộ đồ thể thao": "🎽",
  "Tất thể thao": "🧦",
  "Balo – túi thể thao": "🎒",
  "Phụ kiện thể thao": "⌚",
};

const priceRanges = [
  { label: "Dưới 500.000đ", min: null, max: 500000 },
  { label: "500.000 – 1.000.000đ", min: 500000, max: 1000000 },
  { label: "1.000.000 – 2.000.000đ", min: 1000000, max: 2000000 },
  { label: "2.000.000 – 3.000.000đ", min: 2000000, max: 3000000 },
  { label: "Trên 3.000.000đ", min: 3000000, max: null },
];

const store = useAppStore();
const toast = useToast();
const route = useRoute();
const router = useRouter();

const categories = ref([]);
const filters = reactive({
  cat: "",
  keyword: "",
  min: null,
  max: null,
  sort: "createDate",
  dir: "desc",
  page: 0,
});
const fieldErrors = reactive({});
const products = ref({ content: [], totalPages: 0, number: 0, totalElements: 0 });
const loading = ref(false);
const error = ref("");
const message = ref("");
const syncingRoute = ref(false);

const formatVnd = (value) =>
  Number(value).toLocaleString("vi-VN", { maximumFractionDigits: 0 }) + "đ";

const hasPriceFilter = computed(
  () => Number.isFinite(filters.min) || Number.isFinite(filters.max)
);

const priceFilterLabel = computed(() => {
  if (!hasPriceFilter.value) return "";
  if (Number.isFinite(filters.min) && Number.isFinite(filters.max)) {
    return `${formatVnd(filters.min)} – ${formatVnd(filters.max)}`;
  }
  if (Number.isFinite(filters.min)) return `Từ ${formatVnd(filters.min)}`;
  return `Đến ${formatVnd(filters.max)}`;
});

const totalLabel = computed(() => {
  const total = products.value.totalElements ?? products.value.content?.length ?? 0;
  return `Hiển thị ${products.value.content?.length ?? 0} / ${total} sản phẩm`;
});

const categoryIcon = (name) => categoryIcons[name] || "🏷️";

const samePrice = (a, b) => {
  if (a == null && b == null) return true;
  return Number(a) === Number(b);
};

const isPriceRangeActive = (range) =>
  samePrice(filters.min, range.min) && samePrice(filters.max, range.max);

const selectPriceRange = (range) => {
  filters.min = range.min;
  filters.max = range.max;
  clearFieldError("min");
  clearFieldError("max");
  filters.page = 0;
  fetchProducts();
};

const clearPriceFilter = () => {
  filters.min = null;
  filters.max = null;
  clearFieldError("min");
  clearFieldError("max");
  filters.page = 0;
  fetchProducts();
};

const clearFieldError = (key) => {
  delete fieldErrors[key];
};

const syncUrl = () => {
  syncingRoute.value = true;
  router
    .replace({ path: "/product", query: filtersToRouteQuery(filters) })
    .finally(() => {
      syncingRoute.value = false;
    });
};

const fetchProducts = async () => {
  const validation = validateProductFilters(filters);
  Object.keys(fieldErrors).forEach((k) => delete fieldErrors[k]);
  if (!validation.ok) {
    Object.assign(fieldErrors, validation.errors);
    const first = Object.values(validation.errors)[0];
    error.value = first;
    toast.error(first);
    return;
  }

  filters.keyword = validation.values.keyword;
  filters.min = validation.values.min;
  filters.max = validation.values.max;

  loading.value = true;
  error.value = "";
  try {
    const params = buildProductQueryParams(filters);
    const data = await fetchProductsApi(params);
    const pageData = data.products ?? data;

    products.value = {
      content: pageData.content ?? [],
      totalPages: pageData.totalPages ?? 0,
      number: pageData.number ?? 0,
      totalElements: pageData.totalElements ?? pageData.content?.length ?? 0,
    };

    if (Array.isArray(data.categories) && data.categories.length) {
      categories.value = data.categories.map((c) =>
        typeof c === "string" ? { id: c, name: c } : { id: c.id || c.name, name: c.name || c.id }
      );
    }

    syncUrl();
  } catch (fetchError) {
    console.error("Fetch error:", fetchError);
    const apiMsg =
      fetchError?.response?.data?.message ||
      fetchError?.response?.data?.error ||
      fetchError?.message;
    error.value = apiMsg
      ? `Không thể tải sản phẩm. ${String(apiMsg).slice(0, 200)}`
      : "Không thể tải sản phẩm. Vui lòng thử lại.";
    toast.error(error.value);
    products.value = { content: [], totalPages: 0, number: 0, totalElements: 0 };
  } finally {
    loading.value = false;
  }
};

const selectCategory = (cat) => {
  filters.cat = cat;
  filters.page = 0;
  fetchProducts();
};

const onFilter = () => {
  filters.page = 0;
  fetchProducts();
};

const setSort = (sort, dir) => {
  filters.sort = sort;
  filters.dir = dir;
  filters.page = 0;
  fetchProducts();
};

const changePage = (page) => {
  if (page < 0 || page >= products.value.totalPages) return;
  filters.page = page;
  fetchProducts();
};

const addToCart = (id) => {
  const user = store.state.user;
  if (!user) {
    message.value = "Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng.";
    toast.warning(message.value);
    setTimeout(() => {
      message.value = "";
    }, 2000);
    return;
  }

  const product = products.value.content.find((item) => item.id === id);
  if (!product) return;
  const result = store.addToCart(product, 1);
  if (!result?.success) {
    const msg =
      result?.reason === "out_of_stock"
        ? "Sản phẩm đã hết hàng."
        : result?.reason === "stock_limit"
          ? `Chỉ còn tối đa ${result.stock} sản phẩm trong kho.`
          : "Không thể thêm vào giỏ hàng.";
    message.value = msg;
    toast.error(msg);
    return;
  }
  message.value = `Đã thêm "${product.name}" vào giỏ hàng.`;
  toast.success(message.value);
  setTimeout(() => {
    message.value = "";
  }, 2000);
};

onMounted(() => {
  applyRouteQueryToFilters(route.query, filters);
  fetchProducts();
});

watch(
  () => route.query,
  (query) => {
    if (syncingRoute.value) return;
    applyRouteQueryToFilters(query, filters);
    fetchProducts();
  }
);
</script>
