<template>
  <header class="site-header">
    <div class="header-top">
      <div class="site-container header-top__inner">
        <RouterLink to="/" class="header-logo">Sports4Life</RouterLink>

        <form class="header-search" @submit.prevent="onSearch" ref="searchWrapRef">
          <select v-model="searchCategory" class="header-search__category" aria-label="Danh mục">
            <option value="">Tất cả</option>
            <option value="nam">Giày nam</option>
            <option value="nu">Giày nữ</option>
            <option value="tre-em">Trẻ em</option>
          </select>
          <div class="header-search__field">
            <input
              v-model="searchKeyword"
              type="search"
              class="header-search__input"
              placeholder="Tìm sản phẩm bạn mong muốn"
              autocomplete="off"
              @input="onKeywordInput"
              @focus="onSearchFocus"
              @keydown.down.prevent="highlightNext"
              @keydown.up.prevent="highlightPrev"
              @keydown.enter="onSearchEnter"
              @keydown.esc="closeSuggestions"
            />
            <ul
              v-if="showSuggestions && suggestions.length"
              class="header-search-dropdown"
              role="listbox"
            >
              <li
                v-for="(item, index) in suggestions"
                :key="item.id"
                class="header-search-dropdown__item"
                :class="{ 'is-active': index === highlightIndex }"
                role="option"
                @mousedown.prevent="goToProduct(item.id)"
                @mouseenter="highlightIndex = index"
              >
                <img
                  class="header-search-dropdown__img"
                  :src="item.image || fallbackImage"
                  :alt="item.name"
                />
                <div class="header-search-dropdown__meta">
                  <span class="header-search-dropdown__name">{{ item.name }}</span>
                  <span class="header-search-dropdown__sub">
                    {{ item.brand || item.categoryName || "Sản phẩm" }}
                    · {{ formatPrice(item.price) }}đ
                  </span>
                </div>
              </li>
            </ul>
          </div>
          <button type="submit" class="header-search__btn" aria-label="Tìm kiếm">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <circle cx="11" cy="11" r="7" />
              <path d="M20 20l-3.5-3.5" />
            </svg>
          </button>
        </form>

        <div class="header-actions">
          <RouterLink
            v-if="user"
            to="/profile"
            class="header-icon-btn header-icon-btn--account"
            :aria-label="`Tài khoản ${userDisplayName}`"
            :title="userDisplayName"
          >
            <span class="header-avatar">
              <img v-if="userAvatar" :src="userAvatar" :alt="userDisplayName" />
              <span v-else class="header-avatar__initials">{{ userInitials }}</span>
            </span>
          </RouterLink>

          <template v-else>
            <RouterLink to="/login" class="header-icon-btn" aria-label="Đăng nhập" title="Đăng nhập">
              <svg class="header-icon-btn__svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <circle cx="12" cy="8" r="4" />
                <path d="M5 20c0-3.866 3.134-7 7-7s7 3.134 7 7" stroke-linecap="round" />
              </svg>
              <span class="header-icon-btn__label">Đăng nhập</span>
            </RouterLink>

            <RouterLink to="/register" class="header-icon-btn" aria-label="Đăng ký" title="Đăng ký">
              <svg class="header-icon-btn__svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <circle cx="10" cy="8" r="3.5" />
                <path d="M4 20c0-3.314 2.686-6 6-6" stroke-linecap="round" />
                <path d="M16 11v6M13 14h6" stroke-linecap="round" />
              </svg>
              <span class="header-icon-btn__label">Đăng ký</span>
            </RouterLink>
          </template>

          <RouterLink
            v-if="user"
            to="/favorites"
            class="header-icon-btn header-icon-btn--favorites"
            aria-label="Yêu thích"
            title="Sản phẩm yêu thích"
          >
            <span class="header-icon-btn__wrap">
              <svg class="header-icon-btn__svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path
                  d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
                  stroke-linejoin="round"
                />
              </svg>
              <span v-if="favoriteCount > 0" class="header-icon-btn__badge">{{ favoriteCount > 99 ? "99+" : favoriteCount }}</span>
            </span>
            <span class="header-icon-btn__label">Yêu thích</span>
          </RouterLink>

          <RouterLink to="/cart" class="header-icon-btn header-icon-btn--cart" aria-label="Giỏ hàng" title="Giỏ hàng">
            <span class="header-icon-btn__wrap">
              <svg class="header-icon-btn__svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M6 6h15l-1.5 9h-12L6 6z" stroke-linejoin="round" />
                <path d="M6 6L5 3H2" stroke-linecap="round" stroke-linejoin="round" />
                <circle cx="9" cy="20" r="1.5" fill="currentColor" stroke="none" />
                <circle cx="18" cy="20" r="1.5" fill="currentColor" stroke="none" />
              </svg>
              <span v-if="count > 0" class="header-icon-btn__badge">{{ count > 99 ? "99+" : count }}</span>
            </span>
            <span class="header-icon-btn__label">Giỏ hàng</span>
          </RouterLink>

          <button
            v-if="user"
            type="button"
            class="header-icon-btn header-icon-btn--logout"
            aria-label="Đăng xuất"
            title="Đăng xuất"
            @click="$emit('logout')"
          >
            <svg class="header-icon-btn__svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M9 6V4a1 1 0 0 1 1-1h9a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1h-9a1 1 0 0 1-1-1v-2" stroke-linecap="round" />
              <path d="M14 12H3m0 0l3.5-3.5M3 12l3.5 3.5" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
            <span class="header-icon-btn__label">Thoát</span>
          </button>
        </div>
      </div>
    </div>

    <nav class="header-subnav">
      <div class="site-container header-subnav__inner">
        <div class="header-subnav__links">
          <RouterLink to="/" exact-active-class="is-active">Trang chủ</RouterLink>
          <RouterLink to="/product" active-class="is-active">Sản phẩm</RouterLink>
          <RouterLink to="/featured" active-class="is-active">Nổi bật</RouterLink>
          <RouterLink to="/brands" active-class="is-active">Thương hiệu</RouterLink>
          <RouterLink to="/more" active-class="is-active">Xem thêm</RouterLink>
          <RouterLink to="/contact" active-class="is-active">Liên hệ</RouterLink>
        </div>
        <div class="header-hotline">
          <svg class="header-hotline__svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2z" stroke-linejoin="round" />
          </svg>
          Hotline: <strong>1900 6750</strong>
        </div>
      </div>
    </nav>
  </header>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import { fetchProductSuggestionsApi } from "../services/api";
import { FALLBACK_PRODUCT_IMAGE, resolveProductImage } from "../utils/productImage";

const props = defineProps({
  user: { type: Object, default: null },
  count: { type: Number, default: 0 },
  favoriteCount: { type: Number, default: 0 },
});

defineEmits(["logout"]);

const router = useRouter();
const searchKeyword = ref("");
const searchCategory = ref("");
const suggestions = ref([]);
const showSuggestions = ref(false);
const highlightIndex = ref(-1);
const searchWrapRef = ref(null);
const fallbackImage = FALLBACK_PRODUCT_IMAGE;

let suggestTimer = null;
let suggestRequestId = 0;

const userDisplayName = computed(
  () => props.user?.fullname || props.user?.fullName || props.user?.username || "Tài khoản"
);

const userAvatar = computed(() => props.user?.photo || props.user?.avatar || "");

const userInitials = computed(() => {
  const name = userDisplayName.value.trim();
  const parts = name.split(/\s+/);
  if (parts.length >= 2) {
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }
  return name.slice(0, 2).toUpperCase();
});

const formatPrice = (value) => {
  const n = Number(value) || 0;
  return n.toLocaleString("vi-VN");
};

const closeSuggestions = () => {
  showSuggestions.value = false;
  highlightIndex.value = -1;
};

const fetchSuggestions = async (keyword) => {
  const q = keyword?.trim() || "";
  if (!q) {
    suggestions.value = [];
    showSuggestions.value = false;
    return;
  }

  const requestId = ++suggestRequestId;
  showSuggestions.value = true;
  try {
    const data = await fetchProductSuggestionsApi(q, 8);
    if (requestId !== suggestRequestId) return;
    suggestions.value = (data.suggestions ?? []).map((item) => ({
      ...item,
      image: resolveProductImage(item),
    }));
    highlightIndex.value = -1;
  } catch (err) {
    if (requestId !== suggestRequestId) return;
    console.error("Header suggest error:", err);
    suggestions.value = [];
  }
};

const onKeywordInput = () => {
  clearTimeout(suggestTimer);
  highlightIndex.value = -1;
  const q = searchKeyword.value?.trim() || "";
  if (!q) {
    suggestions.value = [];
    showSuggestions.value = false;
    return;
  }
  suggestTimer = setTimeout(() => fetchSuggestions(q), 220);
};

const onSearchFocus = () => {
  if (searchKeyword.value?.trim() && suggestions.value.length) {
    showSuggestions.value = true;
  } else if (searchKeyword.value?.trim()) {
    fetchSuggestions(searchKeyword.value);
  }
};

const highlightNext = () => {
  if (!suggestions.value.length) return;
  showSuggestions.value = true;
  highlightIndex.value = (highlightIndex.value + 1) % suggestions.value.length;
};

const highlightPrev = () => {
  if (!suggestions.value.length) return;
  showSuggestions.value = true;
  highlightIndex.value =
    highlightIndex.value <= 0 ? suggestions.value.length - 1 : highlightIndex.value - 1;
};

const goToProduct = (id) => {
  closeSuggestions();
  router.push(`/product/${id}`);
};

const onSearchEnter = (event) => {
  if (showSuggestions.value && highlightIndex.value >= 0 && suggestions.value[highlightIndex.value]) {
    event.preventDefault();
    goToProduct(suggestions.value[highlightIndex.value].id);
    return;
  }
  closeSuggestions();
};

const onSearch = () => {
  closeSuggestions();
  router.push({
    path: "/product",
    query: {
      keyword: searchKeyword.value.trim() || undefined,
      cat: searchCategory.value || undefined,
    },
  });
};

const onDocumentClick = (event) => {
  if (!searchWrapRef.value?.contains(event.target)) {
    closeSuggestions();
  }
};

onMounted(() => {
  document.addEventListener("click", onDocumentClick);
});

onBeforeUnmount(() => {
  clearTimeout(suggestTimer);
  document.removeEventListener("click", onDocumentClick);
});
</script>
