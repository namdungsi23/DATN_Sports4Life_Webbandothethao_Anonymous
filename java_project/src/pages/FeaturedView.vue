<template>
  <MainLayout>
    <template #full>
      <div class="page-hero page-hero--featured">
        <img src="https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?w=1400&q=80" alt="Sản phẩm nổi bật" />
        <div class="page-hero__content site-container">
          <p class="page-hero__eyebrow">Editor's pick</p>
          <h1>Sản phẩm nổi bật</h1>
          <p>Những mẫu giày được yêu thích nhất — chọn lọc bởi đội ngũ Sports4Life</p>
        </div>
      </div>
    </template>

    <section class="featured-page-grid">
      <div v-if="loading" class="bestseller-loading">Đang tải...</div>
      <div v-else class="featured-grid featured-grid--page">
        <article v-for="product in allProducts" :key="product.id" class="featured-card">
          <span class="featured-card__badge">NỔI BẬT</span>
          <div class="featured-card__img">
            <ProductImage :product="product" :alt="product.name" />
          </div>
          <div class="featured-card__info">
            <p v-if="product.categoryName" class="featured-card__cat">{{ product.categoryName }}</p>
            <h3>{{ product.name }}</h3>
            <p class="featured-card__price">{{ formatPrice(product.price) }}đ</p>
            <button
              type="button"
              class="featured-card__btn"
              :disabled="!product.inStock"
              @click="onAddToCart(product)"
            >
              {{ product.inStock ? "Thêm vào giỏ" : "Hết hàng" }}
            </button>
          </div>
        </article>
      </div>
    </section>

    <BrandStrip />
    <BestSellerSection />
  </MainLayout>
</template>

<script setup>
import { onMounted, ref } from "vue";
import MainLayout from "../layouts/MainLayout.vue";
import BrandStrip from "../components/BrandStrip.vue";
import BestSellerSection from "../components/BestSellerSection.vue";
import ProductImage from "../components/ProductImage.vue";
import { fetchProductsApi } from "../services/api";
import { useAppStore } from "../stores/appStore";

const { addToCart, state } = useAppStore();
const allProducts = ref([]);
const loading = ref(true);

const formatPrice = (price) => Number(price || 0).toLocaleString("vi-VN");

const onAddToCart = (product) => {
  if (!state.user) return;
  addToCart(product, 1);
};

onMounted(async () => {
  try {
    const data = await fetchProductsApi({ page: 0, sort: "price", dir: "desc" });
    const pageData = data.products ?? data;
    allProducts.value = pageData.content ?? [];
  } catch {
    allProducts.value = [];
  } finally {
    loading.value = false;
  }
});
</script>
