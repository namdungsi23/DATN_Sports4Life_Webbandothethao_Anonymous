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
import { onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import { fetchProductsApi } from "../services/api";
import { useAppStore } from "../stores/appStore";
import ProductImage from "./ProductImage.vue";
import FavoriteButton from "./FavoriteButton.vue";
import { normalizeProduct } from "../utils/productImage";

const { addToCart, state } = useAppStore();
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
    error.value = "Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng.";
    setTimeout(() => {
      error.value = "";
    }, 2500);
    return;
  }
  addToCart(product, 1);
};

onMounted(async () => {
  try {
    const data = await fetchProductsApi({ page: 0, sort: "createDate", dir: "desc" });
    const pageData = data.products ?? data;
    products.value = (pageData.content ?? []).slice(0, 10);
  } catch {
    products.value = [
      { id: 1, name: "Nike Air Zoom Pegasus", price: 2890000, inStock: true, categoryName: "Giày chạy bộ", categoryId: "C001" },
      { id: 2, name: "Adidas Ultraboost 23", price: 3490000, inStock: true, categoryName: "Giày chạy bộ", categoryId: "C001" },
      { id: 3, name: "Puma RS-X", price: 1990000, inStock: true, categoryName: "Lifestyle", categoryId: "C001" },
      { id: 4, name: "New Balance 574", price: 2190000, inStock: true, categoryName: "Classic", categoryId: "C001" },
      { id: 5, name: "Asics Gel-Kayano", price: 3290000, inStock: false, categoryName: "Giày chạy bộ", categoryId: "C001" },
      { id: 6, name: "Converse Chuck 70", price: 1590000, inStock: true, categoryName: "Sneaker", categoryId: "C001" },
    ].map(normalizeProduct);
  } finally {
    loading.value = false;
  }
});
</script>
