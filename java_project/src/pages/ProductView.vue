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
            :key="cat"
            href="#"
            class="product-category__link"
            :class="{ active: filters.cat === cat }"
            @click.prevent="selectCategory(cat)"
          >
            <span>{{ categoryIcon(cat) }}</span>
            {{ cat }}
          </a>
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
              <span v-if="exactMatch" class="product-toolbar__chip">Khớp chính xác</span>
            </p>
          </div>
        </div>

        <form class="product-filter" @submit.prevent="onFilter">
          <div class="product-filter__search" ref="searchWrapRef">
            <span aria-hidden="true">🔍</span>
            <input
              v-model="filters.keyword"
              type="text"
              placeholder="Tìm tên sản phẩm, thương hiệu..."
              autocomplete="off"
              @input="onKeywordInput"
              @focus="onSearchFocus"
              @keydown.down.prevent="highlightNext"
              @keydown.up.prevent="highlightPrev"
              @keydown.enter="onSearchEnter"
              @keydown.esc="closeSuggestions"
            />
            <ul
              v-if="showSuggestions && suggestions.length"
              class="product-search-dropdown"
              role="listbox"
            >
              <li
                v-for="(item, index) in suggestions"
                :key="item.id"
                class="product-search-dropdown__item"
                :class="{ 'is-active': index === highlightIndex }"
                role="option"
                @mousedown.prevent="goToProduct(item.id)"
                @mouseenter="highlightIndex = index"
              >
                <img
                  class="product-search-dropdown__img"
                  :src="item.image || fallbackImage"
                  :alt="item.name"
                />
                <div class="product-search-dropdown__meta">
                  <span class="product-search-dropdown__name">{{ item.name }}</span>
                  <span class="product-search-dropdown__sub">
                    {{ item.brand || item.categoryName || "Sản phẩm" }}
                    · {{ formatPrice(item.price) }}đ
                  </span>
                </div>
              </li>
            </ul>
            <p
              v-else-if="showSuggestions && suggestLoading"
              class="product-search-dropdown product-search-dropdown--empty"
            >
              Đang tìm...
            </p>
            <p
              v-else-if="showSuggestions && filters.keyword.trim() && !suggestLoading"
              class="product-search-dropdown product-search-dropdown--empty"
            >
              Không tìm thấy sản phẩm phù hợp
            </p>
          </div>
          <input v-model.number="filters.min" type="number" class="product-filter__price" placeholder="Giá từ" />
          <input v-model.number="filters.max" type="number" class="product-filter__price" placeholder="Giá đến" />
          <button type="submit" class="product-filter__btn" :disabled="loading">
            {{ loading ? "..." : "Tìm kiếm" }}
          </button>
        </form>

        <div v-if="loading" class="product-loading">
          <div v-for="n in 6" :key="n" class="product-skeleton" />
        </div>
        <template v-else>
          <ProductList
            :products="products"
            :sort="filters.sort"
            :dir="filters.dir"
            @change-page="changePage"
            @add-to-cart="addToCart"
            @sort-change="setSort"
          />

          <section v-if="exactMatch && relatedSuggestions.length" class="product-related-search">
            <h3 class="product-related-search__title">Sản phẩm gợi ý</h3>
            <p class="product-related-search__desc">
              Cùng danh mục hoặc thương hiệu với kết quả tìm kiếm của bạn
            </p>
            <div class="product-related-search__grid">
              <RouterLink
                v-for="item in relatedSuggestions"
                :key="item.id"
                :to="`/product/${item.id}`"
                class="product-related-search__card"
              >
                <img :src="item.image || fallbackImage" :alt="item.name" />
                <div>
                  <strong>{{ item.name }}</strong>
                  <span>{{ formatPrice(item.price) }}đ</span>
                </div>
              </RouterLink>
            </div>
          </section>
        </template>
      </section>
    </div>

    <BrandStrip />
  </MainLayout>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import MainLayout from "../layouts/MainLayout.vue";
import ProductList from "../components/ProductList.vue";
import BrandStrip from "../components/BrandStrip.vue";
import { fetchProductsApi, fetchProductSuggestionsApi } from "../services/api";
import { useAppStore } from "../stores/appStore";
import { FALLBACK_PRODUCT_IMAGE, resolveProductImage } from "../utils/productImage";

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

const { addToCart: addToCartStore } = useAppStore();
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
const products = ref({ content: [], totalPages: 0, number: 0, totalElements: 0 });
const loading = ref(false);
const error = ref("");
const message = ref("");
const exactMatch = ref(false);
const relatedSuggestions = ref([]);

const suggestions = ref([]);
const showSuggestions = ref(false);
const suggestLoading = ref(false);
const highlightIndex = ref(-1);
const searchWrapRef = ref(null);
const fallbackImage = FALLBACK_PRODUCT_IMAGE;

let suggestTimer = null;
let suggestRequestId = 0;

const store = useAppStore();
const route = useRoute();
const router = useRouter();

const totalLabel = computed(() => {
  const total = products.value.totalElements ?? products.value.content?.length ?? 0;
  return `Hiển thị ${products.value.content?.length ?? 0} / ${total} sản phẩm`;
});

const categoryIcon = (name) => categoryIcons[name] || "🏷️";

const formatPrice = (value) => {
  const n = Number(value) || 0;
  return n.toLocaleString("vi-VN");
};

const closeSuggestions = () => {
  showSuggestions.value = false;
  highlightIndex.value = -1;
};

const fetchSuggestions = async (keyword) => {
  const q = keyword?.trim() || "";
  if (!q) {
    suggestions.value = [];
    showSuggestions.value = false;
    return;
  }

  const requestId = ++suggestRequestId;
  suggestLoading.value = true;
  showSuggestions.value = true;
  try {
    const data = await fetchProductSuggestionsApi(q, 8);
    if (requestId !== suggestRequestId) return;
    suggestions.value = (data.suggestions ?? []).map((item) => ({
      ...item,
      image: resolveProductImage(item),
    }));
    highlightIndex.value = -1;
  } catch (err) {
    if (requestId !== suggestRequestId) return;
    console.error("Suggest error:", err);
    suggestions.value = [];
  } finally {
    if (requestId === suggestRequestId) {
      suggestLoading.value = false;
    }
  }
};

const onKeywordInput = () => {
  clearTimeout(suggestTimer);
  highlightIndex.value = -1;
  const q = filters.keyword?.trim() || "";
  if (!q) {
    suggestions.value = [];
    showSuggestions.value = false;
    return;
  }
  suggestTimer = setTimeout(() => fetchSuggestions(q), 220);
};

const onSearchFocus = () => {
  if (filters.keyword?.trim() && suggestions.value.length) {
    showSuggestions.value = true;
  } else if (filters.keyword?.trim()) {
    fetchSuggestions(filters.keyword);
  }
};

const highlightNext = () => {
  if (!suggestions.value.length) return;
  showSuggestions.value = true;
  highlightIndex.value = (highlightIndex.value + 1) % suggestions.value.length;
};

const highlightPrev = () => {
  if (!suggestions.value.length) return;
  showSuggestions.value = true;
  highlightIndex.value =
    highlightIndex.value <= 0 ? suggestions.value.length - 1 : highlightIndex.value - 1;
};

const goToProduct = (id) => {
  closeSuggestions();
  router.push(`/product/${id}`);
};

const onSearchEnter = (event) => {
  // Chỉ vào chi tiết khi đã chọn bằng mũi tên; Enter thường = tìm danh sách
  if (showSuggestions.value && highlightIndex.value >= 0 && suggestions.value[highlightIndex.value]) {
    event.preventDefault();
    goToProduct(suggestions.value[highlightIndex.value].id);
    return;
  }
  closeSuggestions();
};

const fetchProducts = async () => {
  loading.value = true;
  error.value = "";
  try {
    filters.min = filters.min == 0 ? null : filters.min;
    filters.max = filters.max == 0 ? null : filters.max;

    const params = { page: filters.page ?? 0, sort: filters.sort, dir: filters.dir };
    if (filters.cat) params.cat = filters.cat;
    if (filters.keyword?.trim()) params.keyword = filters.keyword.trim();
    if (Number.isFinite(filters.min)) params.min = filters.min;
    if (Number.isFinite(filters.max)) params.max = filters.max;

    const data = await fetchProductsApi(params);
    const pageData = data.products ?? data;

    products.value = {
      content: pageData.content ?? [],
      totalPages: pageData.totalPages ?? 0,
      number: pageData.number ?? 0,
      totalElements: pageData.totalElements ?? pageData.content?.length ?? 0,
    };

    categories.value = data.categories?.map((c) => c.name) || [];
    exactMatch.value = Boolean(data.exactMatch);
    relatedSuggestions.value = (data.suggestions ?? []).map((item) => ({
      ...item,
      image: resolveProductImage(item),
    }));
  } catch (fetchError) {
    console.error("Fetch error:", fetchError);
    const apiMsg =
      fetchError?.response?.data?.message ||
      fetchError?.response?.data?.error ||
      fetchError?.message;
    error.value = apiMsg
      ? `Không thể tải sản phẩm. ${String(apiMsg).slice(0, 200)}`
      : "Không thể tải sản phẩm. Vui lòng thử lại.";
    products.value = { content: [], totalPages: 0, number: 0, totalElements: 0 };
    exactMatch.value = false;
    relatedSuggestions.value = [];
  } finally {
    loading.value = false;
  }
};

const selectCategory = (cat) => {
  filters.cat = cat;
  filters.page = 0;
  closeSuggestions();
  fetchProducts();
};

const onFilter = () => {
  filters.page = 0;
  closeSuggestions();
  const query = {
    ...(filters.keyword?.trim() ? { keyword: filters.keyword.trim() } : {}),
    ...(filters.cat ? { cat: filters.cat } : {}),
  };
  const sameKeyword = String(route.query.keyword || "") === String(query.keyword || "");
  const sameCat = String(route.query.cat || "") === String(query.cat || "");
  if (sameKeyword && sameCat) {
    fetchProducts();
    return;
  }
  router.replace({ path: "/product", query });
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
    setTimeout(() => {
      message.value = "";
    }, 2000);
    return;
  }

  const product = products.value.content.find((item) => item.id === id);
  if (!product) return;
  addToCartStore(product, 1);
  message.value = `Đã thêm "${product.name}" vào giỏ hàng.`;
  setTimeout(() => {
    message.value = "";
  }, 2000);
};

const onDocumentClick = (event) => {
  if (!searchWrapRef.value?.contains(event.target)) {
    closeSuggestions();
  }
};

watch(
  () => [route.query.keyword, route.query.cat],
  ([keyword, cat]) => {
    filters.keyword = keyword ? String(keyword) : "";
    filters.cat = cat ? String(cat) : "";
    filters.page = 0;
    fetchProducts();
  }
);

onMounted(() => {
  if (route.query.keyword) filters.keyword = String(route.query.keyword);
  if (route.query.cat) filters.cat = String(route.query.cat);
  fetchProducts();
  document.addEventListener("click", onDocumentClick);
});

onBeforeUnmount(() => {
  clearTimeout(suggestTimer);
  document.removeEventListener("click", onDocumentClick);
});
</script>
