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
            </p>
          </div>
        </div>

        <form class="product-filter" @submit.prevent="onFilter">
          <div class="product-filter__search">
            <span aria-hidden="true">🔍</span>
            <input
              v-model="filters.keyword"
              type="text"
              placeholder="Tìm tên sản phẩm, thương hiệu..."
            />
          </div>
          <input v-model.number="filters.min" type="number" class="product-filter__price" placeholder="Giá từ" />
          <input v-model.number="filters.max" type="number" class="product-filter__price" placeholder="Giá đến" />
          <button type="submit" class="product-filter__btn" :disabled="loading">
            {{ loading ? "..." : "Lọc" }}
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
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute } from "vue-router";
import MainLayout from "../layouts/MainLayout.vue";
import ProductList from "../components/ProductList.vue";
import BrandStrip from "../components/BrandStrip.vue";
import { fetchProductsApi } from "../services/api";
import { useAppStore } from "../stores/appStore";

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

const store = useAppStore();
const route = useRoute();

const totalLabel = computed(() => {
  const total = products.value.totalElements ?? products.value.content?.length ?? 0;
  return `Hiển thị ${products.value.content?.length ?? 0} / ${total} sản phẩm`;
});

const categoryIcon = (name) => categoryIcons[name] || "🏷️";

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

onMounted(() => {
  if (route.query.keyword) filters.keyword = String(route.query.keyword);
  if (route.query.cat) filters.cat = String(route.query.cat);
  fetchProducts();
});
</script>
