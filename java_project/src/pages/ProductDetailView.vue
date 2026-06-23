<template>
  <MainLayout>
    <div v-if="loading" class="pd-loading site-container">Đang tải sản phẩm...</div>

    <div v-else-if="error" class="pd-error site-container">
      <p>{{ error }}</p>
      <RouterLink to="/product" class="pd-back-link">← Quay lại danh sách</RouterLink>
    </div>

    <template v-else-if="product">
      <nav class="pd-breadcrumb site-container">
        <RouterLink to="/">Trang chủ</RouterLink>
        <span>/</span>
        <RouterLink to="/product">Sản phẩm</RouterLink>
        <span>/</span>
        <span>{{ product.name }}</span>
      </nav>

      <section class="pd-hero site-container">
        <div class="pd-gallery">
          <div class="pd-gallery__main product-img">
            <ProductImage :src="activeImage" :alt="product.name" />
            <FavoriteButton :product="product" variant="icon" size="lg" />
          </div>
          <div v-if="gallery.length > 1" class="pd-gallery__thumbs">
            <button
              v-for="(img, idx) in gallery"
              :key="`${img}-${idx}`"
              type="button"
              class="pd-gallery__thumb"
              :class="{ active: activeImage === img }"
              @click="activeImage = img"
            >
              <img :src="img" :alt="`${product.name} ${idx + 1}`" loading="lazy" />
            </button>
          </div>
        </div>

        <div class="pd-info">
          <p v-if="product.categoryName" class="pd-info__cat">{{ product.categoryName }}</p>
          <h1 class="pd-info__title">{{ product.name }}</h1>

          <div class="pd-price-row">
            <span class="pd-price">{{ formatPrice(displayPrice) }}đ</span>
            <span v-if="product.originalPrice" class="pd-price-old">{{ formatPrice(product.originalPrice) }}đ</span>
            <span v-if="product.discountPercent" class="pd-badge">-{{ product.discountPercent }}%</span>
          </div>

          <p class="pd-installment">Trả góp 0% · Giao hàng 2–4 ngày · Đổi trả 30 ngày</p>

          <div v-if="colorOptions.length" class="pd-option">
            <p class="pd-option__label">Màu sắc: <strong>{{ selectedColor || colorOptions[0] }}</strong></p>
            <div class="pd-swatches">
              <button
                v-for="color in colorOptions"
                :key="color"
                type="button"
                class="pd-swatch"
                :class="{ active: selectedColor === color }"
                @click="selectedColor = color"
              >
                {{ color }}
              </button>
            </div>
          </div>

          <div class="pd-option">
            <p class="pd-option__label">Kích cỡ EU</p>
            <div class="pd-sizes">
              <button
                v-for="size in sizeOptions"
                :key="size"
                type="button"
                class="pd-size"
                :class="{ active: selectedSize === size, disabled: !product.inStock }"
                :disabled="!product.inStock"
                @click="selectedSize = size"
              >
                {{ size }}
              </button>
            </div>
          </div>

          <div class="pd-qty-row">
            <span class="pd-option__label">Số lượng</span>
            <div class="pd-qty">
              <button type="button" aria-label="Giảm" @click="changeQty(-1)">−</button>
              <span>{{ quantity }}</span>
              <button type="button" aria-label="Tăng" @click="changeQty(1)">+</button>
            </div>
          </div>

          <div class="pd-actions">
            <button type="button" class="pd-btn pd-btn--outline" :disabled="!product.inStock" @click="onAddToCart">
              🛒 THÊM VÀO GIỎ
            </button>
            <button type="button" class="pd-btn pd-btn--primary" :disabled="!product.inStock" @click="onBuyNow">
              ⚡ MUA NGAY
            </button>
            <div class="pd-favorite-action">
              <FavoriteButton :product="product" variant="stack" size="lg" />
            </div>
          </div>

          <p v-if="message" class="pd-message">{{ message }}</p>
          <p v-if="!product.inStock" class="pd-stock-warn">Sản phẩm tạm hết hàng</p>
        </div>
      </section>

      <section class="pd-tabs-wrap site-container">
        <div class="pd-tabs">
          <button
            v-for="tab in tabs"
            :key="tab.id"
            type="button"
            class="pd-tab"
            :class="{ active: activeTab === tab.id }"
            @click="activeTab = tab.id"
          >
            {{ tab.label }}
          </button>
        </div>
        <div class="pd-tab-panel">
          <div v-if="activeTab === 'desc'">
            <h3>Mô tả chi tiết</h3>
            <p>{{ product.description || "Sản phẩm thể thao chính hãng, chất liệu cao cấp, phù hợp tập luyện và sử dụng hàng ngày." }}</p>
          </div>
          <div v-else-if="activeTab === 'spec'">
            <h3>Thông số</h3>
            <ul class="pd-spec-list">
              <li><span>Thương hiệu</span><strong>{{ product.brand || "Sports4Life" }}</strong></li>
              <li><span>Danh mục</span><strong>{{ product.categoryName || "—" }}</strong></li>
              <li><span>Tình trạng</span><strong>{{ product.inStock ? "Còn hàng" : "Hết hàng" }}</strong></li>
              <li><span>Mã SP</span><strong>#{{ product.id }}</strong></li>
            </ul>
          </div>
          <div v-else>
            <h3>Chính sách</h3>
            <p>Miễn phí đổi size trong 30 ngày · Bảo hành chính hãng · Hỗ trợ đổi trả nếu lỗi sản xuất.</p>
          </div>
        </div>
      </section>

      <section v-if="related.length" class="pd-related site-container">
        <div class="pd-related__head">
          <h2>Sản phẩm liên quan</h2>
          <RouterLink to="/product">Xem thêm →</RouterLink>
        </div>
        <div class="pd-related__grid">
          <article v-for="item in related" :key="item.id" class="pd-related-card">
            <RouterLink :to="`/product/${item.id}`" class="pd-related-card__img product-img">
              <ProductImage :product="item" :alt="item.name" />
              <FavoriteButton :product="item" variant="icon" size="sm" />
            </RouterLink>
            <div class="pd-related-card__body">
              <RouterLink :to="`/product/${item.id}`" class="pd-related-card__name">{{ item.name }}</RouterLink>
              <p class="pd-related-card__price">{{ formatPrice(item.price) }}đ</p>
            </div>
          </article>
        </div>
      </section>
    </template>
  </MainLayout>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import MainLayout from "../layouts/MainLayout.vue";
import FavoriteButton from "../components/FavoriteButton.vue";
import ProductImage from "../components/ProductImage.vue";
import { fetchProductByIdApi } from "../services/api";
import { useAppStore } from "../stores/appStore";

const route = useRoute();
const router = useRouter();
const { addToCart, state } = useAppStore();

const product = ref(null);
const related = ref([]);
const loading = ref(true);
const error = ref("");
const message = ref("");
const activeImage = ref("");
const activeTab = ref("desc");
const selectedColor = ref("");
const selectedSize = ref("");
const quantity = ref(1);

const tabs = [
  { id: "desc", label: "MÔ TẢ CHI TIẾT" },
  { id: "spec", label: "THÔNG SỐ" },
  { id: "policy", label: "CHÍNH SÁCH" },
];

const gallery = computed(() => {
  const imgs = product.value?.gallery?.length
    ? product.value.gallery
    : product.value?.image
      ? [product.value.image]
      : [];
  return imgs.filter(Boolean);
});

const colorOptions = computed(() => {
  const fromVariants = (product.value?.variants ?? [])
    .map((v) => v.color)
    .filter(Boolean);
  return [...new Set(fromVariants.length ? fromVariants : ["Trắng", "Đen", "Navy", "Xám"])];
});

const sizeOptions = computed(() => {
  const fromVariants = (product.value?.variants ?? [])
    .map((v) => v.size)
    .filter(Boolean);
  return fromVariants.length ? [...new Set(fromVariants)] : ["39", "40", "41", "42", "43", "44"];
});

const displayPrice = computed(() => {
  const variants = product.value?.variants ?? [];
  const match = variants.find(
    (v) =>
      (!selectedSize.value || v.size === selectedSize.value) &&
      (!selectedColor.value || v.color === selectedColor.value)
  );
  return match?.price ?? product.value?.price ?? 0;
});

const formatPrice = (price) => Number(price || 0).toLocaleString("vi-VN");

const initSelections = () => {
  selectedColor.value = colorOptions.value[0] || "";
  selectedSize.value = sizeOptions.value[Math.min(2, sizeOptions.value.length - 1)] || sizeOptions.value[0] || "";
  activeImage.value = gallery.value[0] || product.value?.image || "";
  quantity.value = 1;
};

const loadProduct = async (id) => {
  loading.value = true;
  error.value = "";
  try {
    const data = await fetchProductByIdApi(id);
    product.value = data.product;
    related.value = data.related ?? [];
    initSelections();
  } catch (e) {
    error.value = "Không tìm thấy sản phẩm hoặc đã bị xóa.";
    product.value = null;
    related.value = [];
  } finally {
    loading.value = false;
  }
};

const changeQty = (delta) => {
  quantity.value = Math.max(1, quantity.value + delta);
};

const buildCartProduct = () => ({
  ...product.value,
  price: displayPrice.value,
});

const onAddToCart = () => {
  if (!state.user) {
    message.value = "Vui lòng đăng nhập để thêm vào giỏ hàng.";
    setTimeout(() => { message.value = ""; }, 2500);
    return;
  }
  addToCart(buildCartProduct(), quantity.value);
  message.value = "Đã thêm vào giỏ hàng.";
  setTimeout(() => { message.value = ""; }, 2000);
};

const onBuyNow = () => {
  if (!state.user) {
    router.push("/login");
    return;
  }
  addToCart(buildCartProduct(), quantity.value);
  router.push("/cart/checkout");
};

watch(
  () => route.params.id,
  (id) => {
    if (id) loadProduct(id);
  }
);

onMounted(() => {
  loadProduct(route.params.id);
});
</script>
