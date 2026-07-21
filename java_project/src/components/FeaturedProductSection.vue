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
    <div v-else-if="!products.length" class="bestseller-error">Chưa có sản phẩm nổi bật.</div>

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
import { onMounted, ref, watch } from "vue";
import { RouterLink } from "vue-router";
import { fetchProductsApi } from "../services/api";
import { useAppStore, useToast } from "../stores/appStore";
import ProductImage from "./ProductImage.vue";
import FavoriteButton from "./FavoriteButton.vue";

const props = defineProps({
  initialProducts: { type: Array, default: null },
});

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

const loadProducts = async () => {
  if (Array.isArray(props.initialProducts)) {
    products.value = props.initialProducts;
    loading.value = false;
    return;
  }

  loading.value = true;
  errorMsg.value = "";
  try {
    const data = await fetchProductsApi({ page: 0, sort: "price", dir: "desc" });
    const pageData = data.products ?? data;
    products.value = (pageData.content ?? []).slice(0, 5);
  } catch {
    errorMsg.value = "Không tải được sản phẩm nổi bật. Vui lòng thử lại sau.";
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
      errorMsg.value = "";
    }
  },
  { immediate: false }
);

onMounted(loadProducts);
</script>
