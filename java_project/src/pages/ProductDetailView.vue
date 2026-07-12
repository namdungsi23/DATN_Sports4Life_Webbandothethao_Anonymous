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
            <span class="pd-price">{{ formatPrice(memberPrice) }}đ</span>
            <span v-if="hasRankDiscount" class="pd-price-old">{{ formatPrice(displayPrice) }}đ</span>
          </div>
          <p v-if="memberRankName" class="pd-member-rank">
            Hạng <strong>{{ memberRankName }}</strong>
            <template v-if="rankDiscountPercent"> · giảm {{ rankDiscountPercent }}%</template>
            <template v-if="memberPoints != null"> · {{ memberPoints }} điểm</template>
          </p>

          <p class="pd-installment">Trả góp 0% · Giao hàng 2–4 ngày · Đổi trả 30 ngày</p>

          <!-- ≥2 biến thể: chọn màu/size thật từ DB -->
          <div v-if="realVariants && colorOptions.length" class="pd-option">
            <p class="pd-option__label">
              Màu sắc:
              <strong>{{ selectedColor || "Chọn màu" }}</strong>
            </p>
            <div class="pd-swatches">
              <button
                v-for="color in colorOptions"
                :key="color"
                type="button"
                class="pd-swatch"
                :class="{
                  active: selectedColor === color,
                  disabled: !isColorAvailable(color),
                }"
                :disabled="!isColorAvailable(color)"
                @click="onSelectColor(color)"
              >
                {{ color }}
              </button>
            </div>
            <p v-if="fieldErrors.color" class="pd-field-error">{{ fieldErrors.color }}</p>
          </div>

          <div v-if="realVariants && sizeOptions.length" class="pd-option">
            <p class="pd-option__label">Kích cỡ</p>
            <div class="pd-sizes">
              <button
                v-for="size in sizeOptions"
                :key="size"
                type="button"
                class="pd-size"
                :class="{
                  active: selectedSize === size,
                  disabled: !isSizeAvailable(size),
                }"
                :disabled="!isSizeAvailable(size)"
                @click="onSelectSize(size)"
              >
                {{ size }}
              </button>
            </div>
            <p v-if="fieldErrors.size" class="pd-field-error">{{ fieldErrors.size }}</p>
          </div>

          <p v-if="fieldErrors.variant || fieldErrors.stock" class="pd-field-error">
            {{ fieldErrors.variant || fieldErrors.stock }}
          </p>

          <div class="pd-qty-row">
            <span class="pd-option__label">Số lượng</span>
            <div class="pd-qty">
              <button type="button" aria-label="Giảm" @click="changeQty(-1)">−</button>
              <span>{{ quantity }}</span>
              <button type="button" aria-label="Tăng" :disabled="!canIncreaseQty" @click="changeQty(1)">+</button>
            </div>
            <span v-if="selectedVariant && maxStock != null" class="pd-stock-hint">
              Còn {{ maxStock }} sp
            </span>
          </div>
          <p v-if="fieldErrors.quantity" class="pd-field-error">{{ fieldErrors.quantity }}</p>

          <div class="pd-actions">
            <button
              type="button"
              class="pd-btn pd-btn--outline"
              :disabled="!canPurchase"
              @click="onAddToCart"
            >
              🛒 THÊM VÀO GIỎ
            </button>
            <button
              type="button"
              class="pd-btn pd-btn--primary"
              :disabled="!canPurchase"
              @click="onBuyNow"
            >
              ⚡ MUA NGAY
            </button>
            <div class="pd-favorite-action">
              <FavoriteButton :product="product" variant="stack" size="lg" />
            </div>
          </div>

          <p v-if="message" class="pd-message">{{ message }}</p>
          <p v-if="!canPurchase && product && !loading" class="pd-stock-warn">
            {{ purchaseBlockReason }}
          </p>
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
              <li v-if="selectedVariant?.sku"><span>SKU</span><strong>{{ selectedVariant.sku }}</strong></li>
              <li v-if="selectedColor"><span>Màu</span><strong>{{ selectedColor }}</strong></li>
              <li v-if="selectedSize"><span>Size</span><strong>{{ selectedSize }}</strong></li>
              <li><span>Tình trạng</span><strong>{{ canPurchase ? "Còn hàng" : "Hết hàng / Chưa chọn đủ" }}</strong></li>
              <li><span>Mã SP</span><strong>#{{ product.id }}</strong></li>
            </ul>
          </div>
                    <div v-else-if="activeTab === 'comments'" class="pd-reviews">
            <div class="pd-reviews__summary">
              <div class="pd-reviews__score">
                <strong>{{ Number(avgRating || 0).toFixed(1) }}</strong>
                <span class="pd-reviews__stars">{{ starsLabel(avgRating) }}</span>
                <small>{{ ratingCount }} đánh giá</small>
              </div>
              <p class="pd-reviews__hint">Đăng nhập để gửi bình luận và xếp hạng sản phẩm.</p>
            </div>

            <form v-if="isLoggedIn" class="pd-review-form" @submit.prevent="submitComment">
              <h4>{{ myCommentId ? "Cập nhật đánh giá" : "Viết đánh giá" }}</h4>
              <div class="pd-review-form__stars">
                <button
                  v-for="n in 5"
                  :key="n"
                  type="button"
                  class="pd-star-btn"
                  :class="{ active: commentForm.rating >= n }"
                  @click="commentForm.rating = n"
                >
                  ★
                </button>
                <span>{{ commentForm.rating }}/5</span>
              </div>
              <textarea
                v-model="commentForm.content"
                rows="3"
                maxlength="1000"
                placeholder="Chia sẻ trải nghiệm của bạn về sản phẩm..."
              ></textarea>
              <p v-if="commentError" class="pd-review-form__error">{{ commentError }}</p>
              <button type="submit" class="pd-review-form__submit" :disabled="commentSaving">
                {{ commentSaving ? "Đang gửi..." : myCommentId ? "Cập nhật" : "Gửi đánh giá" }}
              </button>
            </form>
            <p v-else class="pd-reviews__login">
              <RouterLink to="/login">Đăng nhập</RouterLink> để bình luận và xếp hạng sản phẩm.
            </p>

            <ul v-if="comments.length" class="pd-review-list">
              <li v-for="c in comments" :key="c.id" class="pd-review-item">
                <div class="pd-review-item__head">
                  <strong>{{ c.fullName || c.username || "Khách" }}</strong>
                  <span v-if="c.rankName" class="pd-review-item__rank">{{ c.rankName }}</span>
                  <span class="pd-review-item__stars">{{ starsLabel(c.rating) }}</span>
                </div>
                <p class="pd-review-item__content">{{ c.content }}</p>
                <small class="pd-review-item__time">{{ formatCommentTime(c.createdAt) }}</small>
              </li>
            </ul>
            <p v-else class="pd-reviews__empty">Chưa có bình luận nào. Hãy là người đầu tiên!</p>
          </div>
          <div v-else-if="activeTab === 'policy'">
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
import { computed, onMounted, reactive, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import MainLayout from "../layouts/MainLayout.vue";
import FavoriteButton from "../components/FavoriteButton.vue";
import ProductImage from "../components/ProductImage.vue";
import { fetchProductByIdApi, fetchProductCommentsApi, fetchProfileApi, postProductCommentApi } from "../services/api";
import { useAppStore, useToast } from "../stores/appStore";
import {
  colorsForSize,
  findVariant,
  hasRealVariants,
  isVariantInStock,
  sizesForColor,
  uniqueColors,
  uniqueSizes,
  validateVariantSelection,
} from "../utils/variantSelection";

const route = useRoute();
const router = useRouter();
const store = useAppStore();
const toast = useToast();

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
const fieldErrors = reactive({});

const comments = ref([]);
const avgRating = ref(0);
const ratingCount = ref(0);
const myCommentId = ref(null);
const commentSaving = ref(false);
const commentError = ref("");
const commentForm = reactive({
  rating: 5,
  content: "",
});

const isLoggedIn = computed(() => Boolean(store.state.user?.username));
const memberRankName = computed(() => store.state.user?.rankName || "");
const memberPoints = computed(() =>
  store.state.user?.totalPoint != null ? Number(store.state.user.totalPoint) : null
);
const rankDiscountPercent = computed(() => {
  const n = Number(store.state.user?.rankDiscountPercent);
  return Number.isFinite(n) && n > 0 ? n : 0;
});
const hasRankDiscount = computed(() => isLoggedIn.value && rankDiscountPercent.value > 0);

const starsLabel = (n) => {
  const v = Math.max(0, Math.min(5, Math.round(Number(n) || 0)));
  return "★".repeat(v) + "☆".repeat(5 - v);
};

const formatCommentTime = (value) => {
  if (!value) return "";
  return new Date(value).toLocaleString("vi-VN");
};

const loadComments = async () => {
  const id = product.value?.id || route.params.id;
  if (!id) return;
  try {
    const data = await fetchProductCommentsApi(id);
    comments.value = data.comments || [];
    avgRating.value = Number(data.avgRating || 0);
    ratingCount.value = Number(data.ratingCount || 0);
    const me = store.state.user?.username;
    const mine = comments.value.find((c) => c.username === me);
    if (mine) {
      myCommentId.value = mine.id;
      commentForm.rating = mine.rating || 5;
      commentForm.content = mine.content || "";
    } else {
      myCommentId.value = null;
    }
  } catch {
    comments.value = [];
  }
};

const syncMemberRank = async () => {
  if (!store.state.user?.username) return;
  try {
    const data = await fetchProfileApi();
    const profile = data?.profile || data;
    if (!profile || typeof profile !== "object") return;
    store.updateUserProfile({
      rankId: profile.rankId,
      rankName: profile.rankName,
      totalPoint: profile.totalPoint,
      rankDiscountPercent: profile.rankDiscountPercent,
      rankMinPoint: profile.rankMinPoint,
      fullname: profile.fullname || profile.fullName,
      photo: profile.photo || profile.avatar,
      email: profile.email,
    });
  } catch {
    /* optional */
  }
};

const submitComment = async () => {
  commentError.value = "";
  if (!commentForm.content?.trim()) {
    commentError.value = "Vui lòng nhập nội dung bình luận.";
    return;
  }
  commentSaving.value = true;
  try {
    const data = await postProductCommentApi({
      productId: Number(product.value.id),
      rating: commentForm.rating,
      content: commentForm.content.trim(),
    });
    avgRating.value = Number(data.avgRating || 0);
    ratingCount.value = Number(data.ratingCount || 0);
    await loadComments();
    toast.success(data.message || "Đã gửi đánh giá.");
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "Gửi đánh giá thất bại.";
    commentError.value = msg;
    toast.error(msg);
  } finally {
    commentSaving.value = false;
  }
};

const tabs = [
  { id: "desc", label: "MÔ TẢ CHI TIẾT" },
  { id: "spec", label: "THÔNG SỐ" },
  { id: "comments", label: "ĐÁNH GIÁ" },
  { id: "policy", label: "CHÍNH SÁCH" },
];

const variants = computed(() => product.value?.variants ?? []);
const realVariants = computed(() => hasRealVariants(variants.value));

const colorOptions = computed(() =>
  realVariants.value ? uniqueColors(variants.value) : []
);

const sizeOptions = computed(() => {
  if (!realVariants.value) return [];
  if (selectedColor.value) {
    return sizesForColor(variants.value, selectedColor.value);
  }
  return uniqueSizes(variants.value);
});

const selectedVariant = computed(() => {
  if (!variants.value.length) return null;
  if (realVariants.value) {
    return findVariant(variants.value, {
      color: selectedColor.value,
      size: selectedSize.value,
    });
  }
  return variants.value.find((v) => v.isDefault) || variants.value[0];
});

const gallery = computed(() => {
  const variantImgs = Array.isArray(selectedVariant.value?.images)
    ? selectedVariant.value.images.filter(Boolean)
    : [];
  if (variantImgs.length) return variantImgs;

  const productGallery = Array.isArray(product.value?.gallery)
    ? product.value.gallery.filter(Boolean)
    : [];
  if (productGallery.length) return productGallery;

  return product.value?.image ? [product.value.image] : [];
});

const displayPrice = computed(
  () => selectedVariant.value?.price ?? product.value?.price ?? 0
);

const memberPrice = computed(() => {
  const base = Number(displayPrice.value) || 0;
  if (!hasRankDiscount.value) return base;
  return Math.round(base * (1 - rankDiscountPercent.value / 100));
});

const maxStock = computed(() => {
  const v = selectedVariant.value;
  if (!v) return null;
  const n = Number(v.stock ?? v.quantity);
  return Number.isFinite(n) ? n : null;
});

const canIncreaseQty = computed(() => {
  if (maxStock.value == null) return true;
  return quantity.value < maxStock.value;
});

const canPurchase = computed(() => {
  if (!product.value) return false;
  if (realVariants.value) {
    if (colorOptions.value.length && !selectedColor.value) return false;
    if (sizeOptions.value.length && !selectedSize.value) return false;
    if (!selectedVariant.value) return false;
    return isVariantInStock(selectedVariant.value);
  }
  if (selectedVariant.value) return isVariantInStock(selectedVariant.value);
  return product.value.inStock !== false;
});

const purchaseBlockReason = computed(() => {
  if (realVariants.value) {
    if (colorOptions.value.length && !selectedColor.value) return "Vui lòng chọn màu sắc.";
    if (sizeOptions.value.length && !selectedSize.value) return "Vui lòng chọn kích cỡ.";
    if (!selectedVariant.value) return "Không có biến thể phù hợp.";
    if (!isVariantInStock(selectedVariant.value)) return "Biến thể đã hết hàng.";
  }
  return "Sản phẩm tạm hết hàng";
});

const formatPrice = (price) => Number(price || 0).toLocaleString("vi-VN");

const clearFieldErrors = () => {
  Object.keys(fieldErrors).forEach((k) => delete fieldErrors[k]);
};

const isColorAvailable = (color) => {
  const sizes = sizesForColor(variants.value, color);
  if (!sizes.length) {
    return variants.value.some(
      (v) => v.color === color && isVariantInStock(v)
    );
  }
  if (selectedSize.value && !sizes.includes(selectedSize.value)) {
    return variants.value.some(
      (v) =>
        v.color === color &&
        (!selectedSize.value || v.size === selectedSize.value) &&
        isVariantInStock(v)
    );
  }
  return variants.value.some((v) => v.color === color && isVariantInStock(v));
};

const isSizeAvailable = (size) => {
  const match = findVariant(variants.value, {
    color: selectedColor.value,
    size,
  });
  if (match) return isVariantInStock(match);
  return variants.value.some(
    (v) =>
      v.size === size &&
      (!selectedColor.value || v.color === selectedColor.value) &&
      isVariantInStock(v)
  );
};

const onSelectColor = (color) => {
  selectedColor.value = color;
  delete fieldErrors.color;
  delete fieldErrors.variant;
  delete fieldErrors.stock;
  const allowed = sizesForColor(variants.value, color);
  if (selectedSize.value && !allowed.includes(selectedSize.value)) {
    selectedSize.value = allowed[0] || "";
  }
};

const onSelectSize = (size) => {
  selectedSize.value = size;
  delete fieldErrors.size;
  delete fieldErrors.variant;
  delete fieldErrors.stock;
  const allowed = colorsForSize(variants.value, size);
  if (selectedColor.value && !allowed.includes(selectedColor.value)) {
    selectedColor.value = allowed[0] || "";
  }
};

const syncGalleryToVariant = () => {
  const imgs = gallery.value;
  if (!imgs.length) return;
  if (!imgs.includes(activeImage.value)) {
    activeImage.value = imgs[0];
  }
};

const initSelections = () => {
  clearFieldErrors();
  if (realVariants.value) {
    selectedColor.value = colorOptions.value[0] || "";
    const sizes = selectedColor.value
      ? sizesForColor(variants.value, selectedColor.value)
      : uniqueSizes(variants.value);
    selectedSize.value = sizes[0] || "";
  } else {
    selectedColor.value = "";
    selectedSize.value = "";
  }
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
    await Promise.all([loadComments(), syncMemberRank()]);
  } catch (e) {
    error.value = "Không tìm thấy sản phẩm hoặc đã bị xóa.";
    product.value = null;
    related.value = [];
    comments.value = [];
  } finally {
    loading.value = false;
  }
};

const changeQty = (delta) => {
  delete fieldErrors.quantity;
  let next = Math.max(1, quantity.value + delta);
  if (maxStock.value != null) next = Math.min(next, maxStock.value);
  quantity.value = next;
};

const buildCartProduct = (variant) => ({
  ...product.value,
  price: variant?.price ?? displayPrice.value,
  image: variant?.image || variant?.images?.[0] || product.value?.image,
  variantId: variant?.id ?? null,
  size: selectedSize.value || variant?.size || "",
  color: selectedColor.value || variant?.color || "",
  stock: variant?.stock ?? variant?.quantity ?? product.value?.stock,
  quantity: variant?.stock ?? variant?.quantity,
});

const runValidation = () => {
  clearFieldErrors();
  const result = validateVariantSelection({
    variants: variants.value,
    color: selectedColor.value,
    size: selectedSize.value,
    quantity: quantity.value,
  });
  if (!result.ok) {
    Object.assign(fieldErrors, result.errors);
    const first = Object.values(result.errors)[0];
    toast.error(first);
  }
  return result;
};

const onAddToCart = () => {
  if (!store.state.user) {
    message.value = "Vui lòng đăng nhập để thêm vào giỏ hàng.";
    toast.warning(message.value);
    setTimeout(() => {
      message.value = "";
    }, 2500);
    return;
  }

  const result = runValidation();
  if (!result.ok) return;

  const cartResult = store.addToCart(buildCartProduct(result.variant), quantity.value);
  if (!cartResult?.success) {
    const msg =
      cartResult?.reason === "out_of_stock"
        ? "Sản phẩm đã hết hàng."
        : cartResult?.reason === "stock_limit"
          ? `Chỉ còn tối đa ${cartResult.stock} sản phẩm trong kho.`
          : "Không thể thêm vào giỏ hàng.";
    message.value = msg;
    toast.error(msg);
    return;
  }

  message.value = "Đã thêm vào giỏ hàng.";
  toast.success(message.value);
  setTimeout(() => {
    message.value = "";
  }, 2000);
};

const onBuyNow = () => {
  if (!store.state.user) {
    router.push("/login");
    return;
  }
  const result = runValidation();
  if (!result.ok) return;

  const cartResult = store.addToCart(buildCartProduct(result.variant), quantity.value);
  if (!cartResult?.success) {
    toast.error("Không thể thêm vào giỏ hàng.");
    return;
  }
  router.push("/cart/checkout");
};

watch([selectedColor, selectedSize, selectedVariant], () => {
  syncGalleryToVariant();
  if (maxStock.value != null && quantity.value > maxStock.value) {
    quantity.value = Math.max(1, maxStock.value);
  }
});

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
