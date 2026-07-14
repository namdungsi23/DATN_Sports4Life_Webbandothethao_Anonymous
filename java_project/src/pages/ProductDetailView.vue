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
        <div class="pd-gallery" :class="{ 'pd-gallery--grid': useGridGallery }">
          <!-- Adidas-style 2×2 gallery -->
          <template v-if="useGridGallery">
            <div class="pd-gallery__grid-wrap product-img">
              <div class="pd-gallery__grid">
                <button
                  v-for="(img, idx) in gallery.slice(0, 4)"
                  :key="`${img}-${idx}`"
                  type="button"
                  class="pd-gallery__cell"
                  :class="{ active: activeImage === img }"
                  @click="activeImage = img"
                >
                  <img :src="img" :alt="`${product.name} ${idx + 1}`" loading="lazy" />
                </button>
              </div>
              <FavoriteButton :product="product" variant="icon" size="lg" />
            </div>
          </template>
          <template v-else>
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
          </template>
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

          <!-- Biến thể kiểu Adidas: colorway + lưới size -->
          <div v-if="realVariants && colorWays.length" class="pd-option pd-option--color">
            <div class="pd-option__head">
              <p class="pd-option__label">
                Màu
                <strong>{{ selectedColor || "—" }}</strong>
              </p>
              <span class="pd-option__meta">{{ colorWays.length }} tuỳ chọn</span>
            </div>
            <div class="pd-colorways" role="listbox" aria-label="Chọn màu">
              <button
                v-for="way in colorWays"
                :key="way.color"
                type="button"
                class="pd-colorway"
                :class="{
                  active: selectedColor === way.color,
                  disabled: !way.available,
                  'pd-colorway--swatch': !way.image,
                }"
                :disabled="!way.available"
                :title="way.color"
                :aria-label="way.color"
                :aria-selected="selectedColor === way.color"
                :style="way.swatchStyle"
                @click="onSelectColor(way.color)"
              >
                <img v-if="way.image" :src="way.image" :alt="way.color" loading="lazy" />
                <span v-else class="pd-colorway__label">{{ way.color }}</span>
              </button>
            </div>
            <p v-if="fieldErrors.color" class="pd-field-error">{{ fieldErrors.color }}</p>
          </div>

          <div v-if="realVariants && sizeOptions.length" class="pd-option pd-option--size">
            <div class="pd-option__head">
              <p class="pd-option__label">
                {{ selectedSize ? `Size — ${selectedSize}` : "Chọn size" }}
              </p>
              <button type="button" class="pd-size-guide" @click.prevent="showSizeGuide = !showSizeGuide">
                Bảng size
              </button>
            </div>
            <p v-if="showSizeGuide" class="pd-size-guide-hint">
              Giày: EU 39–43 · Áo/quần/áo khoác: S–XL · Phụ kiện/balo/tất: Free · Đổi size trong 30 ngày.
            </p>
            <div class="pd-size-grid" role="listbox" aria-label="Chọn size">
              <button
                v-for="size in sizeOptions"
                :key="size"
                type="button"
                class="pd-size-tile"
                :class="{
                  active: selectedSize === size,
                  soldout: !isSizeAvailable(size),
                }"
                :aria-selected="selectedSize === size"
                :aria-disabled="!isSizeAvailable(size)"
                @click="onSelectSize(size)"
              >
                <span>{{ size }}</span>
              </button>
            </div>
            <p v-if="fieldErrors.size" class="pd-field-error pd-field-error--box">{{ fieldErrors.size }}</p>
          </div>

          <p v-if="fieldErrors.variant || fieldErrors.stock" class="pd-field-error pd-field-error--box">
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
              class="pd-btn pd-btn--primary"
              @click="onAddToCart"
            >
              Thêm vào túi
            </button>
            <button
              type="button"
              class="pd-btn pd-btn--dark"
              @click="onBuyNow"
            >
              Mua ngay
            </button>
            <div class="pd-favorite-action">
              <FavoriteButton :product="product" variant="stack" size="lg" />
            </div>
          </div>

          <p v-if="message" class="pd-message">{{ message }}</p>
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
            @click="toggleTab(tab.id)"
          >
            {{ tab.label }}
          </button>
        </div>
        <div v-if="activeTab" class="pd-tab-panel">
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
                placeholder="Nội dung đánh giá"
                :class="{ 'is-invalid': commentError }"
                @input="commentError = ''"
              ></textarea>
              <p v-if="commentError" class="pd-review-form__error">{{ commentError }}</p>
              <button type="submit" class="pd-review-form__submit" :disabled="commentSaving">
                {{ commentSaving ? "Đang gửi..." : myCommentId ? "Cập nhật" : "Gửi đánh giá" }}
              </button>
            </form>
            <p v-else class="pd-reviews__login">
              <RouterLink to="/login">Đăng nhập</RouterLink> để bình luận.
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
            <p v-else class="pd-reviews__empty">Chưa có bình luận.</p>
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
  isFreeSize,
  isVariantInStock,
  selectableSizes,
  sizesForColor,
  uniqueColors,
  validateVariantSelection,
} from "../utils/variantSelection";
import { firstError, getApiError, runValidation as validateFields } from "../utils/validators";

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
const activeTab = ref("");
const selectedColor = ref("");
const selectedSize = ref("");
const quantity = ref(1);
const showSizeGuide = ref(false);
const fieldErrors = reactive({});

const toggleTab = (id) => {
  activeTab.value = activeTab.value === id ? "" : id;
};

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
  const result = validateFields(commentForm, {
    content: [
      "required",
      { type: "min", min: 5, message: "Nội dung tối thiểu 5 ký tự." },
      { type: "max", max: 1000, message: "Nội dung tối đa 1000 ký tự." },
    ],
    rating: [{ type: "number", min: 1, max: 5, message: "Chọn xếp hạng từ 1 đến 5." }],
  });
  if (!result.ok) {
    commentError.value = firstError(result.errors);
    toast.error(commentError.value);
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
    const api = getApiError(err, "Gửi đánh giá thất bại.");
    commentError.value = api.message;
    toast.error(api.message);
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

/** Hex gợi ý theo tên màu — dùng khi ảnh colorway trùng / thiếu. */
const colorSwatchHex = (color) => {
  const c = String(color || "")
    .trim()
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d");
  if (c.includes("den") || c === "black") return "#111111";
  if (c.includes("trang") || c === "white") return "#f5f5f5";
  if (c.includes("navy")) return "#1b2a4a";
  if (c.includes("do") || c === "red") return "#c8102e";
  if (c.includes("xam") || c.includes("grey") || c.includes("gray")) return "#8a8a8a";
  if (c.includes("xanh la") || c.includes("green")) return "#2d6a4f";
  if (c.includes("xanh")) return "#1d4e89";
  return "#d1d1d1";
};

/** Một biến thể đại diện cho màu — ưu tiên mặc định / có ảnh, không gộp nhiều size. */
const colorRepresentative = (color) => {
  const ofColor = variants.value.filter(
    (v) => String(v.color || "").trim() === String(color || "").trim()
  );
  if (!ofColor.length) return null;
  return (
    ofColor.find((v) => v.isDefault && Array.isArray(v.images) && v.images.length) ||
    ofColor.find((v) => Array.isArray(v.images) && v.images.length) ||
    ofColor.find((v) => v.isDefault) ||
    ofColor[0]
  );
};

const colorOptions = computed(() =>
  realVariants.value ? uniqueColors(variants.value) : []
);

/**
 * Colorway: mỗi màu 1 ảnh đại diện = góc 1 của biến thể đúng màu đó.
 * Click màu → gallery = đúng 4 góc của màu đó.
 */
const imagesForColor = (color) => {
  const takeFour = (imgs) =>
    (Array.isArray(imgs) ? imgs : []).filter(Boolean).slice(0, 4);
  const rep = colorRepresentative(color);
  const fromRep = takeFour(rep?.images);
  if (fromRep.length) return fromRep;
  // Fallback: lấy ảnh từ bất kỳ BT cùng màu
  const ofColor = variants.value.filter(
    (v) => String(v.color || "").trim() === String(color || "").trim()
  );
  for (const v of ofColor) {
    const imgs = takeFour(v.images);
    if (imgs.length) return imgs;
  }
  return [];
};

const colorWays = computed(() => {
  return colorOptions.value.map((color) => {
    const imgs = imagesForColor(color);
    const image = imgs[0] || "";
    const hex = colorSwatchHex(color);
    return {
      color,
      image,
      images: imgs,
      available: isColorAvailable(color),
      hex,
      swatchStyle: image
        ? undefined
        : {
            backgroundColor: hex,
            borderColor: hex === "#f5f5f5" ? "#ccc" : "#000",
          },
    };
  });
});

const sizeOptions = computed(() => {
  if (!realVariants.value) return [];
  return selectableSizes(variants.value, selectedColor.value || "");
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

/** Gallery: luôn 4 góc của màu/biến thể đang chọn — không trộn màu khác. */
const gallery = computed(() => {
  const takeFour = (imgs) =>
    (Array.isArray(imgs) ? imgs : []).filter(Boolean).slice(0, 4);

  // Ưu tiên biến thể khớp màu+size
  if (selectedVariant.value) {
    const own = takeFour(selectedVariant.value.images);
    if (own.length) return own;
  }

  // Đã chọn màu → 4 góc đúng màu đó
  if (selectedColor.value) {
    const byColor = imagesForColor(selectedColor.value);
    if (byColor.length) return byColor;
  }

  const def =
    variants.value.find((v) => v.isDefault) || variants.value[0] || null;
  const fromDefault = takeFour(def?.images);
  if (fromDefault.length) return fromDefault;

  return takeFour(product.value?.gallery).length
    ? takeFour(product.value?.gallery)
    : product.value?.image
      ? [product.value.image]
      : [];
});

/** Adidas-style 2×2 khi biến thể có đủ 4 góc. */
const useGridGallery = computed(() => gallery.value.length >= 4);

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
    // Size Free: không bắt chọn trên UI
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
  if (allowed.length && allowed.every(isFreeSize)) {
    selectedSize.value = allowed[0];
  } else if (selectedSize.value && !allowed.includes(selectedSize.value)) {
    const stillOk = selectableSizes(variants.value, color).find((s) =>
      isSizeAvailable(s)
    );
    selectedSize.value = stillOk || "";
  } else if (allowed.length === 1) {
    selectedSize.value = allowed[0];
  }
  // Luôn nhảy về góc 1 của đúng màu vừa chọn
  const imgs = imagesForColor(color);
  activeImage.value = imgs[0] || product.value?.image || "";
};

const onSelectSize = (size) => {
  if (!isSizeAvailable(size)) {
    fieldErrors.size = "Size này đã hết hàng. Vui lòng chọn size khác.";
    toast.warning(fieldErrors.size);
    return;
  }
  selectedSize.value = size;
  delete fieldErrors.size;
  delete fieldErrors.variant;
  delete fieldErrors.stock;
  const allowed = colorsForSize(variants.value, size);
  if (selectedColor.value && !allowed.includes(selectedColor.value)) {
    selectedColor.value = allowed[0] || "";
  }
  const v = findVariant(variants.value, {
    color: selectedColor.value,
    size,
  });
  const imgs = (Array.isArray(v?.images) ? v.images : []).filter(Boolean).slice(0, 4);
  activeImage.value =
    imgs[0] ||
    imagesForColor(selectedColor.value)[0] ||
    product.value?.image ||
    "";
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
  showSizeGuide.value = false;
  if (realVariants.value) {
    const def = variants.value.find((v) => v.isDefault);
    const defColor = def && String(def.color || "").trim();
    selectedColor.value =
      (defColor && colorOptions.value.includes(defColor) ? defColor : "") ||
      colorOptions.value[0] ||
      "";

    const sizesOfColor = sizesForColor(variants.value, selectedColor.value);
    if (sizesOfColor.length && sizesOfColor.every(isFreeSize)) {
      // Phụ kiện / balo / tất: tự gán Free
      selectedSize.value = sizesOfColor[0];
    } else if (sizesOfColor.length === 1) {
      selectedSize.value = sizesOfColor[0];
    } else {
      // Giày / áo: user tự chọn size (không pre-select)
      selectedSize.value = "";
    }
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
  activeTab.value = "";
  showSizeGuide.value = false;
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

watch([selectedColor, selectedSize, selectedVariant, gallery], () => {
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
