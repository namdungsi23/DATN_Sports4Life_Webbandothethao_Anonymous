<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { RouterLink } from "vue-router";
import { fetchProductByIdApi } from "../services/api";
import { useAppStore, useToast } from "../stores/appStore";
import FavoriteButton from "./FavoriteButton.vue";
import { resolveProductImage } from "../utils/productImage";

const props = defineProps({
  open: { type: Boolean, default: false },
  product: { type: Object, default: null },
});

const emit = defineEmits(["close", "added"]);

const store = useAppStore();
const toast = useToast();

const loading = ref(false);
const error = ref("");
const detail = ref(null);
const activeIndex = ref(0);
const direction = ref(1);
const adding = ref(false);

const displayProduct = computed(() => detail.value || props.product);

const gallery = computed(() => {
  const p = displayProduct.value;
  if (!p) return [];
  const list = Array.isArray(p.gallery) ? p.gallery.filter(Boolean) : [];
  if (list.length) return [...new Set(list)].slice(0, 6);
  const one = resolveProductImage(p);
  return one ? [one] : [];
});

const activeSrc = computed(
  () => gallery.value[activeIndex.value] || resolveProductImage(displayProduct.value)
);

const slideName = computed(() =>
  direction.value >= 0 ? "qv-slide-next" : "qv-slide-prev"
);

const formatPrice = (price) => Number(price || 0).toLocaleString("vi-VN");

const loadDetail = async () => {
  const id = props.product?.id;
  if (!id) return;

  loading.value = true;
  error.value = "";
  activeIndex.value = 0;
  detail.value = null;

  try {
    const data = await fetchProductByIdApi(id);
    detail.value = data?.product || props.product;
  } catch {
    detail.value = props.product;
    error.value = "";
  } finally {
    loading.value = false;
  }
};

watch(
  () => [props.open, props.product?.id],
  ([isOpen]) => {
    if (isOpen && props.product?.id) {
      loadDetail();
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = "";
    }
  }
);

const onKeydown = (e) => {
  if (!props.open) return;
  if (e.key === "Escape") emit("close");
  if (e.key === "ArrowLeft") prev();
  if (e.key === "ArrowRight") next();
};

onMounted(() => window.addEventListener("keydown", onKeydown));
onUnmounted(() => {
  window.removeEventListener("keydown", onKeydown);
  document.body.style.overflow = "";
});

const setIndex = (idx) => {
  if (idx < 0 || idx >= gallery.value.length || idx === activeIndex.value) return;
  direction.value = idx > activeIndex.value ? 1 : -1;
  activeIndex.value = idx;
};

const prev = () => {
  const len = gallery.value.length;
  if (len < 2) return;
  direction.value = -1;
  activeIndex.value = (activeIndex.value - 1 + len) % len;
};

const next = () => {
  const len = gallery.value.length;
  if (len < 2) return;
  direction.value = 1;
  activeIndex.value = (activeIndex.value + 1) % len;
};

const onAddToCart = () => {
  const p = displayProduct.value;
  if (!p?.inStock) return;

  if (!store.state.user) {
    toast.warning("Vui lòng đăng nhập để thêm vào giỏ hàng.");
    return;
  }

  adding.value = true;
  try {
    const result = store.addToCart(p, 1);
    if (!result?.success) {
      toast.error(result?.reason === "out_of_stock" ? "Sản phẩm đã hết hàng." : "Không thể thêm vào giỏ.");
      return;
    }
    toast.success(`Đã thêm "${p.name}" vào giỏ.`);
    emit("added", p);
  } finally {
    adding.value = false;
  }
};

const close = () => emit("close");
</script>

<template>
  <Teleport to="body">
    <Transition name="qv">
      <div v-if="open && product" class="qv" @click.self="close">
        <div class="qv__box" role="dialog" aria-modal="true" aria-label="Xem nhanh sản phẩm">
          <button type="button" class="qv__close" aria-label="Đóng" @click="close">×</button>

          <div class="qv__body">
            <div class="qv__gallery">
              <div class="qv__main product-img">
                <div class="qv__stage">
                  <Transition :name="slideName">
                    <img
                      :key="activeSrc"
                      :src="activeSrc"
                      :alt="displayProduct?.name || ''"
                      class="qv__main-img"
                    />
                  </Transition>
                </div>

                <template v-if="gallery.length > 1">
                  <button type="button" class="qv__nav qv__nav--prev" aria-label="Ảnh trước" @click="prev">
                    ‹
                  </button>
                  <button type="button" class="qv__nav qv__nav--next" aria-label="Ảnh sau" @click="next">
                    ›
                  </button>
                </template>

                <FavoriteButton
                  v-if="displayProduct"
                  :product="displayProduct"
                  variant="icon"
                  size="sm"
                />
              </div>

              <div v-if="gallery.length > 1" class="qv__thumbs">
                <button
                  v-for="(img, idx) in gallery"
                  :key="`${img}-${idx}`"
                  type="button"
                  class="qv__thumb"
                  :class="{ active: idx === activeIndex }"
                  @click="setIndex(idx)"
                >
                  <img :src="img" :alt="`${displayProduct?.name || ''} ${idx + 1}`" loading="lazy" />
                </button>
              </div>
            </div>

            <div class="qv__info">
              <p v-if="loading" class="qv__loading">Đang tải...</p>

              <template v-else-if="displayProduct">
                <p v-if="displayProduct.categoryName" class="qv__cat">
                  {{ displayProduct.categoryName }}
                </p>
                <h2 class="qv__title">{{ displayProduct.name }}</h2>
                <p class="qv__price">{{ formatPrice(displayProduct.price) }}đ</p>

                <p v-if="displayProduct.description" class="qv__desc">
                  {{ displayProduct.description }}
                </p>
                <p v-else class="qv__desc qv__desc--muted">
                  Xem chi tiết để chọn màu, size và thông tin đầy đủ.
                </p>

                <p class="qv__stock" :class="{ 'is-out': !displayProduct.inStock }">
                  {{ displayProduct.inStock ? "Còn hàng" : "Hết hàng" }}
                </p>

                <div class="qv__actions">
                  <button
                    type="button"
                    class="qv__cart"
                    :disabled="!displayProduct.inStock || adding"
                    @click="onAddToCart"
                  >
                    {{ displayProduct.inStock ? "Thêm vào giỏ" : "Hết hàng" }}
                  </button>
                  <RouterLink
                    :to="`/product/${displayProduct.id}`"
                    class="qv__detail"
                    @click="close"
                  >
                    Xem chi tiết
                  </RouterLink>
                </div>
              </template>

              <p v-if="error" class="qv__error">{{ error }}</p>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.qv {
  position: fixed;
  inset: 0;
  z-index: 1400;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(15, 23, 22, 0.55);
  backdrop-filter: blur(3px);
}

.qv__box {
  position: relative;
  width: min(860px, 100%);
  max-height: min(90vh, 720px);
  overflow: auto;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 28px 70px rgba(0, 0, 0, 0.32);
}

.qv__close {
  position: absolute;
  top: 10px;
  right: 12px;
  z-index: 3;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: #f3f4f6;
  color: #4b5563;
  font-size: 24px;
  line-height: 1;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}

.qv__close:hover {
  background: #e5e7eb;
  color: #111;
}

.qv__body {
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  gap: 0;
  min-height: 360px;
}

.qv__gallery {
  padding: 24px 20px 24px 24px;
  background: linear-gradient(160deg, #f7f8f7 0%, #eef1ef 100%);
}

.qv__main {
  position: relative;
  height: 320px;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.qv__stage {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.qv__main-img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
  will-change: transform, opacity;
}

.qv-slide-next-enter-active,
.qv-slide-next-leave-active,
.qv-slide-prev-enter-active,
.qv-slide-prev-leave-active {
  position: absolute;
  inset: 0;
  transition: transform 0.35s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.28s ease;
}

.qv-slide-next-enter-from {
  transform: translateX(24%);
  opacity: 0;
}

.qv-slide-next-leave-to {
  transform: translateX(-16%);
  opacity: 0;
}

.qv-slide-prev-enter-from {
  transform: translateX(-24%);
  opacity: 0;
}

.qv-slide-prev-leave-to {
  transform: translateX(16%);
  opacity: 0;
}

.qv__nav {
  position: absolute;
  top: 50%;
  z-index: 4;
  transform: translateY(-50%);
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.95);
  color: #1a3c34;
  font-size: 20px;
  cursor: pointer;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.12);
}

.qv__nav--prev {
  left: 10px;
}

.qv__nav--next {
  right: 10px;
}

.qv__nav:hover {
  background: #fff;
}

.qv__thumbs {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.qv__thumb {
  flex: 0 0 58px;
  width: 58px;
  height: 58px;
  padding: 0;
  border: 2px solid transparent;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.15s ease, transform 0.15s ease;
}

.qv__thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.qv__thumb:hover {
  transform: translateY(-1px);
}

.qv__thumb.active {
  border-color: #dc2626;
}

.qv__info {
  padding: 36px 28px 28px;
  display: flex;
  flex-direction: column;
}

.qv__loading {
  margin: 0;
  color: #6b7280;
  font-size: 14px;
}

.qv__cat {
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #9ca3af;
}

.qv__title {
  margin: 0 0 12px;
  font-size: 22px;
  line-height: 1.3;
  color: #1a3c34;
  padding-right: 28px;
}

.qv__price {
  margin: 0 0 16px;
  font-size: 24px;
  font-weight: 700;
  color: #dc2626;
}

.qv__desc {
  margin: 0 0 16px;
  font-size: 14px;
  line-height: 1.55;
  color: #4b5563;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.qv__desc--muted {
  color: #9ca3af;
}

.qv__stock {
  margin: 0 0 20px;
  font-size: 13px;
  font-weight: 600;
  color: #059669;
}

.qv__stock.is-out {
  color: #9ca3af;
}

.qv__actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: auto;
}

.qv__cart {
  height: 46px;
  border: none;
  border-radius: 10px;
  background: #dc2626;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s ease;
}

.qv__cart:hover:not(:disabled) {
  background: #b91c1c;
}

.qv__cart:disabled {
  background: #d1d5db;
  cursor: not-allowed;
}

.qv__detail {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 44px;
  border: 1.5px solid #1a3c34;
  border-radius: 10px;
  color: #1a3c34;
  text-decoration: none;
  font-size: 14px;
  font-weight: 600;
  transition: background 0.15s ease, color 0.15s ease;
}

.qv__detail:hover {
  background: #1a3c34;
  color: #fff;
}

.qv__error {
  margin: 12px 0 0;
  color: #dc2626;
  font-size: 13px;
}

.qv-enter-active,
.qv-leave-active {
  transition: opacity 0.22s ease;
}

.qv-enter-active .qv__box,
.qv-leave-active .qv__box {
  transition: transform 0.22s ease, opacity 0.22s ease;
}

.qv-enter-from,
.qv-leave-to {
  opacity: 0;
}

.qv-enter-from .qv__box,
.qv-leave-to .qv__box {
  opacity: 0;
  transform: translateY(14px) scale(0.98);
}

@media (max-width: 760px) {
  .qv {
    padding: 12px;
    align-items: flex-end;
  }

  .qv__box {
    max-height: 92vh;
    border-radius: 16px 16px 12px 12px;
  }

  .qv__body {
    grid-template-columns: 1fr;
  }

  .qv__gallery {
    padding: 20px 16px 8px;
  }

  .qv__main {
    height: 240px;
  }

  .qv__info {
    padding: 16px 18px 22px;
  }

  .qv__title {
    font-size: 18px;
  }
}
</style>
