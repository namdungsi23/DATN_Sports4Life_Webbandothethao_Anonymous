<template>
  <header class="site-header">
    <div class="header-top">
      <div class="site-container header-top__inner">
        <RouterLink to="/" class="header-logo" aria-label="Sports4Life trang chủ">
          <img
            :src="BRAND.logoUrl"
            alt="Sports4Life"
            class="header-logo__img"
            width="180"
            height="40"
          />
        </RouterLink>

        <div class="header-search-wrap" ref="searchWrap">
          <form class="header-search" @submit.prevent="onSearch">
            <select v-model="searchCategory" class="header-search__category" aria-label="Danh mục">
              <option value="">Tất cả</option>
              <option v-for="cat in categories" :key="cat.id || cat.name" :value="cat.name">
                {{ cat.name }}
              </option>
            </select>
            <input
              v-model="searchKeyword"
              type="search"
              class="header-search__input"
              :class="{ 'is-invalid': searchError }"
              :maxlength="KEYWORD_MAX"
              placeholder="Tìm sản phẩm bạn mong muốn"
              autocomplete="off"
              @input="onSearchInput"
              @focus="onSearchFocus"
            />
            <button type="submit" class="header-search__btn" aria-label="Tìm kiếm">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <circle cx="11" cy="11" r="7" />
                <path d="M20 20l-3.5-3.5" />
              </svg>
            </button>
          </form>

          <div v-if="suggestOpen" class="header-suggest">
            <p v-if="suggestLoading" class="header-suggest__state">Đang tìm…</p>
            <template v-else-if="suggestions.length">
              <button
                v-for="item in suggestions"
                :key="item.id"
                type="button"
                class="header-suggest__item"
                @click="goSuggestion(item)"
              >
                <img
                  class="header-suggest__img"
                  :src="item.image || FALLBACK_IMG"
                  :alt="item.name"
                  @error="onSuggestImgError"
                />
                <span class="header-suggest__info">
                  <span class="header-suggest__name">{{ item.name }}</span>
                  <span v-if="item.brand" class="header-suggest__brand">{{ item.brand }}</span>
                </span>
                <span v-if="item.price" class="header-suggest__price">{{ formatPrice(item.price) }}đ</span>
              </button>
              <button type="button" class="header-suggest__all" @click="onSearch">
                Xem tất cả kết quả cho “{{ searchKeyword.trim() }}”
              </button>
            </template>
            <p v-else-if="searchKeyword.trim().length >= 2" class="header-suggest__state">
              Không tìm thấy sản phẩm phù hợp
            </p>
          </div>
        </div>

        <div class="header-actions">
          <div v-if="user" class="header-notify" ref="bellWrap">
            <button
              type="button"
              class="header-icon-btn header-icon-btn--notify"
              aria-label="Thông báo"
              title="Thông báo"
              @click.stop="toggleBell"
            >
              <span class="header-icon-btn__wrap">
                <svg class="header-icon-btn__svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M6 9a6 6 0 0 1 12 0c0 7 3 8 3 8H3s3-1 3-8" stroke-linejoin="round" />
                  <path d="M10 19a2 2 0 0 0 4 0" stroke-linecap="round" />
                </svg>
                <span v-if="unreadCount > 0" class="header-icon-btn__badge">
                  {{ unreadCount > 99 ? "99+" : unreadCount }}
                </span>
              </span>
              <span class="header-icon-btn__label">Thông báo</span>
            </button>
            <div v-if="bellOpen" class="header-notify__dropdown" @click.stop>
              <div class="header-notify__head">
                <strong>Thông báo</strong>
                <button
                  v-if="unreadCount > 0"
                  type="button"
                  class="header-notify__all"
                  @click="markAllRead"
                >
                  Đọc tất cả
                </button>
              </div>
              <p v-if="!notifications.length" class="header-notify__empty">Chưa có thông báo</p>
              <button
                v-for="n in notifications"
                :key="n.id"
                type="button"
                class="header-notify__item"
                :class="{ 'is-unread': isUnread(n) }"
                @click="openNotification(n)"
              >
                <strong>{{ n.title }}</strong>
                <span>{{ n.message }}</span>
                <small>{{ formatTime(n.createdAt) }}</small>
              </button>
            </div>
          </div>

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

          <div
            class="header-nav-drop"
            :class="{ 'is-open': productMenuOpen }"
            ref="productMenuWrap"
            @mouseenter="openProductMenu"
            @mouseleave="closeProductMenu"
          >
            <button
              type="button"
              class="header-nav-drop__trigger"
              :class="{ 'is-active': isProductRoute }"
              aria-haspopup="true"
              :aria-expanded="productMenuOpen"
              @click.stop="toggleProductMenu"
            >
              Sản phẩm
              <svg class="header-nav-drop__chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <path d="M6 9l6 6 6-6" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </button>
            <div v-show="productMenuOpen" class="header-mega" role="menu">
              <div class="site-container header-mega__inner">
                <RouterLink
                  to="/product"
                  class="header-mega__item header-mega__item--all"
                  role="menuitem"
                  @click="closeProductMenu"
                >
                  Tất cả sản phẩm
                </RouterLink>
                <RouterLink
                  v-for="cat in categories"
                  :key="cat.id || cat.name"
                  :to="{ path: '/product', query: { cat: cat.name } }"
                  class="header-mega__item"
                  role="menuitem"
                  @click="closeProductMenu"
                >
                  {{ cat.name }}
                </RouterLink>
                <p v-if="!categories.length" class="header-mega__empty">Chưa có danh mục</p>
              </div>
            </div>
          </div>

          <RouterLink to="/featured" active-class="is-active">Nổi bật</RouterLink>
          <RouterLink to="/brands" active-class="is-active">Thương hiệu</RouterLink>
          <RouterLink to="/more" active-class="is-active">Xem thêm</RouterLink>
          <RouterLink to="/contact" active-class="is-active">Liên hệ</RouterLink>
        </div>
        <div class="header-hotline">
          <svg class="header-hotline__svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2z" stroke-linejoin="round" />
          </svg>
          Hotline: <strong>0336 694 988</strong>
        </div>
      </div>
    </nav>
  </header>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import {
  fetchCategoriesApi,
  fetchProductsApi,
  fetchUserNotificationsApi,
  fetchUserUnreadCountApi,
  markAllUserNotificationsReadApi,
  markUserNotificationReadApi,
} from "../services/api";
import { useToast } from "../stores/appStore";
import { KEYWORD_MAX, validateProductFilters } from "../utils/productFilter";
import { BRAND } from "../utils/brand";

const props = defineProps({
  user: { type: Object, default: null },
  count: { type: Number, default: 0 },
  favoriteCount: { type: Number, default: 0 },
});

defineEmits(["logout"]);

const router = useRouter();
const route = useRoute();
const toast = useToast();
const searchKeyword = ref("");
const searchCategory = ref("");
const categories = ref([]);
const searchError = ref("");

const productMenuWrap = ref(null);
const productMenuOpen = ref(false);

const isProductRoute = computed(() => {
  const p = route.path || "";
  return p === "/product" || p.startsWith("/product/");
});

const openProductMenu = () => {
  productMenuOpen.value = true;
};

const closeProductMenu = () => {
  productMenuOpen.value = false;
};

const toggleProductMenu = () => {
  productMenuOpen.value = !productMenuOpen.value;
};

const FALLBACK_IMG = "/images/placeholder-product.png";
const searchWrap = ref(null);
const suggestions = ref([]);
const suggestOpen = ref(false);
const suggestLoading = ref(false);
let suggestTimer = null;

const formatPrice = (price) => Number(price || 0).toLocaleString("vi-VN");

const onSuggestImgError = (e) => {
  if (e?.target) e.target.src = FALLBACK_IMG;
};

const fetchSuggestions = async () => {
  const keyword = searchKeyword.value.trim();
  if (keyword.length < 2) {
    suggestions.value = [];
    suggestLoading.value = false;
    return;
  }
  suggestLoading.value = true;
  try {
    const data = await fetchProductsApi({
      keyword,
      cat: searchCategory.value || undefined,
      page: 0,
    });
    const pageData = data.products ?? data;
    suggestions.value = (pageData.content ?? []).slice(0, 6).map((p) => ({
      id: p.id,
      name: p.name,
      brand: p.brand,
      price: p.price ?? p.minPrice ?? p.displayPrice,
      image: p.image || p.thumbnail || (Array.isArray(p.images) ? p.images[0] : ""),
    }));
  } catch {
    suggestions.value = [];
  } finally {
    suggestLoading.value = false;
  }
};

const onSearchInput = () => {
  searchError.value = "";
  suggestOpen.value = true;
  if (suggestTimer) clearTimeout(suggestTimer);
  suggestTimer = setTimeout(fetchSuggestions, 300);
};

const onSearchFocus = () => {
  if (searchKeyword.value.trim().length >= 2 && suggestions.value.length) {
    suggestOpen.value = true;
  }
};

const goSuggestion = (item) => {
  suggestOpen.value = false;
  if (item?.id != null) {
    router.push(`/product/${item.id}`);
  }
};

const bellWrap = ref(null);
const bellOpen = ref(false);
const notifications = ref([]);
const unreadCount = ref(0);
let pollTimer = null;

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

const onSearch = () => {
  const validation = validateProductFilters({ keyword: searchKeyword.value });
  if (!validation.ok) {
    searchError.value = validation.errors.keyword || "Từ khóa không hợp lệ";
    toast.error(searchError.value);
    return;
  }
  searchError.value = "";
  suggestOpen.value = false;
  router.push({
    path: "/product",
    query: {
      keyword: validation.values.keyword || undefined,
      cat: searchCategory.value || undefined,
    },
  });
};

const isUnread = (n) => n && n.isRead !== true && n.read !== true;

const formatTime = (value) => {
  if (!value) return "";
  return new Date(value).toLocaleString("vi-VN");
};

const loadUnread = async () => {
  if (!props.user) {
    unreadCount.value = 0;
    return;
  }
  try {
    const data = await fetchUserUnreadCountApi();
    unreadCount.value = Number(data.unreadCount || 0);
  } catch {
    /* ignore */
  }
};

const loadNotifications = async () => {
  try {
    const data = await fetchUserNotificationsApi(20);
    notifications.value = data.notifications || [];
    unreadCount.value = Number(data.unreadCount || 0);
  } catch {
    notifications.value = [];
  }
};

const toggleBell = async () => {
  bellOpen.value = !bellOpen.value;
  if (bellOpen.value) {
    await loadNotifications();
  }
};

const openNotification = async (n) => {
  try {
    if (isUnread(n) && n.id != null) {
      await markUserNotificationReadApi(n.id);
      n.isRead = true;
      unreadCount.value = Math.max(0, unreadCount.value - 1);
    }
  } catch {
    /* vẫn điều hướng */
  }
  bellOpen.value = false;
  const link = typeof n?.link === "string" ? n.link.trim() : "";
  if (link) {
    try {
      await router.push(link.startsWith("/") ? link : `/${link}`);
    } catch {
      /* ignore */
    }
  }
};

const markAllRead = async () => {
  try {
    await markAllUserNotificationsReadApi();
    notifications.value = notifications.value.map((n) => ({ ...n, isRead: true }));
    unreadCount.value = 0;
  } catch {
    /* ignore */
  }
};

const onDocClick = (e) => {
  if (bellWrap.value && !bellWrap.value.contains(e.target)) {
    bellOpen.value = false;
  }
  if (searchWrap.value && !searchWrap.value.contains(e.target)) {
    suggestOpen.value = false;
  }
  if (productMenuWrap.value && !productMenuWrap.value.contains(e.target)) {
    productMenuOpen.value = false;
  }
};

const startPolling = () => {
  if (pollTimer) clearInterval(pollTimer);
  if (!props.user) return;
  loadUnread();
  pollTimer = setInterval(loadUnread, 20000);
};

watch(
  () => route.fullPath,
  () => {
    productMenuOpen.value = false;
    suggestOpen.value = false;
  }
);

watch(
  () => props.user,
  (u) => {
    if (!u) {
      bellOpen.value = false;
      notifications.value = [];
      unreadCount.value = 0;
      if (pollTimer) clearInterval(pollTimer);
      pollTimer = null;
      return;
    }
    startPolling();
  }
);

onMounted(async () => {
  document.addEventListener("click", onDocClick);
  startPolling();
  try {
    const data = await fetchCategoriesApi();
    categories.value = (data || []).map((c) =>
      typeof c === "string" ? { id: c, name: c } : { id: c.id || c.name, name: c.name || c.id }
    );
  } catch (err) {
    console.warn("Không tải được danh mục header", err);
    categories.value = [];
  }
});

onUnmounted(() => {
  document.removeEventListener("click", onDocClick);
  if (pollTimer) clearInterval(pollTimer);
  if (suggestTimer) clearTimeout(suggestTimer);
});
</script>
