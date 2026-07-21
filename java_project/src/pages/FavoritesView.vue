<template>
  <MainLayout>
    <div class="favorites-page">
      <div class="favorites-page__head">
        <h1>Sản phẩm yêu thích</h1>
        <p>{{ favoriteItems.length ? `${favoriteCount} sản phẩm` : "Chưa có sản phẩm nào" }}</p>
      </div>

      <div v-if="loading" class="favorites-loading">Đang tải danh sách yêu thích...</div>

      <div v-else-if="!favoriteItems.length" class="favorites-empty">
        <div class="favorites-empty__icon">❤️</div>
        <h2>Danh sách yêu thích trống</h2>
        <p>Bấm biểu tượng trái tim trên sản phẩm để lưu lại những mẫu giày bạn thích.</p>
        <RouterLink to="/product" class="favorites-empty__btn">Khám phá sản phẩm</RouterLink>
      </div>

      <div v-else class="favorites-grid">
        <article v-for="item in favoriteItems" :key="item.id" class="favorites-card">
          <div class="favorites-card__img">
            <RouterLink :to="`/product/${item.id}`">
              <ProductImage :src="item.image" :alt="item.name" />
            </RouterLink>
            <button
              type="button"
              class="favorites-card__remove"
              aria-label="Bỏ yêu thích"
              title="Bỏ yêu thích"
              :disabled="removingId === item.id"
              @click="onRemove(item.id)"
            >
              ×
            </button>
          </div>

          <div class="favorites-card__body">
            <p v-if="item.brand" class="favorites-card__brand">{{ item.brand }}</p>
            <RouterLink :to="`/product/${item.id}`" class="favorites-card__name">
              <h3>{{ item.name }}</h3>
            </RouterLink>
            <p class="favorites-card__price">{{ formatPrice(item.price) }}đ</p>
            <p v-if="!item.inStock" class="favorites-card__soldout">Hết hàng</p>

            <div class="favorites-card__actions">
              <RouterLink :to="`/product/${item.id}`" class="favorites-card__view">Xem chi tiết</RouterLink>
              <button
                v-if="item.inStock"
                type="button"
                class="favorites-card__cart"
                @click="addToCart(item)"
              >
                Thêm vào giỏ
              </button>
            </div>
          </div>
        </article>
      </div>
    </div>
  </MainLayout>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import MainLayout from "../layouts/MainLayout.vue";
import ProductImage from "../components/ProductImage.vue";
import { useAppStore } from "../stores/appStore";

const store = useAppStore();
const loading = ref(true);
const removingId = ref(null);

const favoriteItems = computed(() => store.state.favorites);
const favoriteCount = computed(() => store.favoriteCount.value);

const formatPrice = (price) => Number(price || 0).toLocaleString("vi-VN");

const addToCart = (item) => {
  store.addToCart(item);
};

const onRemove = async (productId) => {
  removingId.value = productId;
  await store.removeFavorite(productId);
  removingId.value = null;
};

onMounted(async () => {
  await store.loadFavorites();
  loading.value = false;
});
</script>
