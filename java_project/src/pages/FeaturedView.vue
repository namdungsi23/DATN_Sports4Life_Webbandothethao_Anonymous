<template>
  <MainLayout>
    <template #full>
      <div class="page-hero page-hero--featured">
        <img
          src="https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?w=1600&q=80"
          alt="Sản phẩm nổi bật Sports4Life"
        />
        <div class="page-hero__content site-container">
          <p class="page-hero__brand">Sports4Life</p>
          <p class="page-hero__eyebrow">Editor's pick</p>
          <h1>Nổi bật</h1>
          <p>Những mẫu được yêu thích nhất — chọn lọc cho phong cách năng động</p>
          <RouterLink to="/product" class="page-hero__cta">Khám phá tất cả</RouterLink>
        </div>
      </div>
    </template>

    <section class="featured-page">
      <header class="featured-page__intro">
        <h2>Bộ sưu tập chọn lọc</h2>
        <p>{{ loading ? "Đang tải..." : `${allProducts.length} sản phẩm nổi bật` }}</p>
      </header>

      <div v-if="loading" class="featured-page__loading">Đang tải sản phẩm...</div>

      <div v-else-if="allProducts.length" class="featured-editorial">
        <article
          v-for="(product, index) in allProducts"
          :key="product.id"
          class="featured-editorial__item"
          :class="{ 'featured-editorial__item--hero': index === 0 }"
        >
          <ProductCardMedia
            class="featured-editorial__media"
            :product="product"
            @quick-view="openQuickView"
          >
            <FavoriteButton :product="product" variant="icon" :size="index === 0 ? 'md' : 'sm'" />
          </ProductCardMedia>
          <div class="featured-editorial__body">
            <p v-if="product.categoryName" class="featured-editorial__cat">{{ product.categoryName }}</p>
            <RouterLink :to="`/product/${product.id}`" class="featured-editorial__name">
              <h3>{{ product.name }}</h3>
            </RouterLink>
            <p class="featured-editorial__price">{{ formatPrice(product.price) }}đ</p>
            <button
              type="button"
              class="featured-editorial__btn"
              :disabled="!product.inStock"
              @click="onAddToCart(product)"
            >
              {{ product.inStock ? "Thêm vào giỏ" : "Hết hàng" }}
            </button>
          </div>
        </article>
      </div>

      <p v-else class="featured-page__empty">Chưa có sản phẩm nổi bật.</p>
    </section>

    <BestSellerSection />

    <QuickViewModal
      :open="!!quickViewProduct"
      :product="quickViewProduct"
      @close="quickViewProduct = null"
    />
  </MainLayout>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import MainLayout from "../layouts/MainLayout.vue";
import BestSellerSection from "../components/BestSellerSection.vue";
import FavoriteButton from "../components/FavoriteButton.vue";
import ProductCardMedia from "../components/ProductCardMedia.vue";
import QuickViewModal from "../components/QuickViewModal.vue";
import { fetchProductsApi } from "../services/api";
import { useAppStore, useToast } from "../stores/appStore";

const store = useAppStore();
const toast = useToast();
const allProducts = ref([]);
const loading = ref(true);
const quickViewProduct = ref(null);

const openQuickView = (product) => {
  quickViewProduct.value = product;
};

const formatPrice = (price) => Number(price || 0).toLocaleString("vi-VN");

const onAddToCart = (product) => {
  if (!store.state.user) {
    toast.warning("Vui lòng đăng nhập để thêm vào giỏ hàng.");
    return;
  }
  const result = store.addToCart(product, 1);
  if (!result?.success) {
    toast.error(
      result?.reason === "out_of_stock"
        ? "Sản phẩm đã hết hàng."
        : result?.reason === "stock_limit"
          ? `Chỉ còn tối đa ${result.stock} sản phẩm.`
          : "Không thể thêm vào giỏ hàng."
    );
    return;
  }
  toast.success(`Đã thêm "${product.name}" vào giỏ.`);
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
