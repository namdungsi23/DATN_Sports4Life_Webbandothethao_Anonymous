<template>
  <section class="featured-section">
    <div class="section-head">
      <div>
        <p class="section-head__eyebrow">Editor's pick</p>
        <h2 class="section-head__title">Sản phẩm nổi bật</h2>
      </div>
      <RouterLink to="/featured" class="section-head__link">Xem tất cả →</RouterLink>
    </div>

    <div v-if="loading" class="bestseller-loading">Đang tải sản phẩm...</div>
    <div v-else-if="errorMsg" class="bestseller-error">{{ errorMsg }}</div>

    <div v-else class="featured-editorial featured-editorial--home">
      <article
        v-for="(product, index) in products"
        :key="product.id"
        class="featured-editorial__item"
        :class="{ 'featured-editorial__item--hero': index === 0 }"
      >
        <div class="featured-editorial__media product-img">
          <RouterLink :to="`/product/${product.id}`">
            <ProductImage :product="product" :alt="product.name" />
          </RouterLink>
          <FavoriteButton :product="product" variant="icon" size="sm" />
        </div>
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
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import { fetchProductsApi } from "../services/api";
import { useAppStore, useToast } from "../stores/appStore";
import ProductImage from "./ProductImage.vue";
import FavoriteButton from "./FavoriteButton.vue";

const store = useAppStore();
const toast = useToast();
const products = ref([]);
const loading = ref(true);
const errorMsg = ref("");

const formatPrice = (price) => Number(price || 0).toLocaleString("vi-VN");

const onAddToCart = (product) => {
  if (!store.state.user) {
    toast.warning("Vui lòng đăng nhập để thêm vào giỏ hàng.");
    return;
  }
  const result = store.addToCart(product, 1);
  if (!result?.success) {
    toast.error("Không thể thêm vào giỏ hàng.");
    return;
  }
  toast.success(`Đã thêm "${product.name}" vào giỏ.`);
};

const mockProducts = () => [
  {
    id: 101,
    name: "Adidas Ultraboost Light",
    price: 4290000,
    inStock: true,
    categoryName: "Chạy bộ",
    image: "https://images.unsplash.com/photo-1606107557195-0f29cb4c6adc?w=600&q=80",
  },
  {
    id: 102,
    name: "Nike Air Max 90",
    price: 3190000,
    inStock: true,
    categoryName: "Lifestyle",
    image: "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=400&q=80",
  },
  {
    id: 103,
    name: "Puma Suede Classic",
    price: 1890000,
    inStock: true,
    categoryName: "Classic",
    image: "https://images.unsplash.com/photo-1608231387042-66d1773070a5?w=400&q=80",
  },
  {
    id: 104,
    name: "New Balance 550",
    price: 2790000,
    inStock: true,
    categoryName: "Retro",
    image: "https://images.unsplash.com/photo-1605348532760-67531229fd64?w=400&q=80",
  },
  {
    id: 105,
    name: "Converse Run Star Hike",
    price: 2190000,
    inStock: true,
    categoryName: "Sneaker",
    image: "https://images.unsplash.com/photo-1607522370275-f142006118a7?w=400&q=80",
  },
];

onMounted(async () => {
  try {
    const data = await fetchProductsApi({ page: 0, sort: "price", dir: "desc" });
    const pageData = data.products ?? data;
    const list = pageData.content ?? [];
    products.value = list.length ? list.slice(0, 5) : mockProducts();
  } catch {
    products.value = mockProducts();
  } finally {
    loading.value = false;
  }
});
</script>
