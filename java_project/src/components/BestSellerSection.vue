<template>
  <section class="bestseller-section">
    <div class="section-head">
      <div>
        <p class="section-head__eyebrow">Top bán chạy</p>
        <h2 class="section-head__title">Sản phẩm bán chạy</h2>
      </div>
      <RouterLink to="/product" class="section-head__link">Xem tất cả →</RouterLink>
    </div>

    <div v-if="loading" class="bestseller-loading">Đang tải sản phẩm...</div>
    <div v-else-if="error" class="bestseller-error">{{ error }}</div>
    <div v-else-if="!products.length" class="bestseller-error">Chưa có sản phẩm để hiển thị.</div>

    <div v-else class="bestseller-scroll">
      <button
        type="button"
        class="bestseller-scroll__arrow bestseller-scroll__arrow--prev"
        aria-label="Cuộn trái"
        @click="scrollBy(-320)"
      >
        ‹
      </button>

      <div ref="trackRef" class="bestseller-track">
        <article v-for="(product, index) in products" :key="product.id" class="bestseller-card">
          <span v-if="index < 3" class="bestseller-card__badge">HOT</span>
          <div class="bestseller-card__img-wrap product-img">
            <RouterLink :to="`/product/${product.id}`">
              <ProductImage :product="product" :alt="product.name" />
            </RouterLink>
            <FavoriteButton :product="product" variant="icon" size="sm" />
          </div>
          <div class="bestseller-card__body">
            <p v-if="product.categoryName" class="bestseller-card__cat">{{ product.categoryName }}</p>
            <RouterLink :to="`/product/${product.id}`" class="bestseller-card__name">
              <h3>{{ product.name }}</h3>
            </RouterLink>
            <p class="bestseller-card__price">{{ formatPrice(product.price) }}đ</p>
            <button
              type="button"
              class="bestseller-card__btn"
              :disabled="!product.inStock"
              @click="onAddToCart(product)"
            >
              {{ product.inStock ? "Thêm vào giỏ" : "Hết hàng" }}
            </button>
          </div>
        </article>
      </div>

      <button
        type="button"
        class="bestseller-scroll__arrow bestseller-scroll__arrow--next"
        aria-label="Cuộn phải"
        @click="scrollBy(320)"
      >
        ›
      </button>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref, watch } from "vue";
import { RouterLink } from "vue-router";
import { fetchProductsApi } from "../services/api";
import { useAppStore, useToast } from "../stores/appStore";
import ProductImage from "./ProductImage.vue";
import FavoriteButton from "./FavoriteButton.vue";

const props = defineProps({
  initialProducts: { type: Array, default: null },
});

const { addToCart, state } = useAppStore();
const toast = useToast();
const trackRef = ref(null);
const products = ref([]);
const loading = ref(true);
const error = ref("");

const formatPrice = (price) => Number(price || 0).toLocaleString("vi-VN");

const scrollBy = (offset) => {
  trackRef.value?.scrollBy({ left: offset, behavior: "smooth" });
};

const onAddToCart = (product) => {
  if (!state.user) {
    toast.warning("Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng.");
    return;
  }
  addToCart(product, 1);
};

const loadProducts = async () => {
  if (Array.isArray(props.initialProducts)) {
    products.value = props.initialProducts;
    loading.value = false;
    return;
  }

  loading.value = true;
  error.value = "";
  try {
    const data = await fetchProductsApi({ page: 0, sort: "createDate", dir: "desc" });
    const pageData = data.products ?? data;
    products.value = (pageData.content ?? []).slice(0, 10);
  } catch {
    error.value = "Không tải được sản phẩm bán chạy. Vui lòng thử lại sau.";
  } finally {
    loading.value = false;
  }
};

watch(
  () => props.initialProducts,
  (value) => {
    if (Array.isArray(value)) {
      products.value = value;
      loading.value = false;
      error.value = "";
    }
  },
  { immediate: false }
);

onMounted(loadProducts);
</script>
