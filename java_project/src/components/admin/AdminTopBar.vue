<template>
  <header class="admin-topbar">
    <div class="admin-topbar__left">
      <button
        type="button"
        class="admin-topbar__toggle"
        aria-label="Mở menu"
        @click="$emit('toggle-sidebar')"
      >
        <AdminIcon name="menu" :size="20" />
      </button>
      <div>
        <h1 class="admin-topbar__title">{{ title }}</h1>
        <p v-if="subtitle" class="admin-topbar__subtitle">{{ subtitle }}</p>
      </div>
    </div>

    <div class="admin-topbar__right">
      <LiveClock variant="admin" />
      <div class="admin-topbar__search" ref="searchWrap">
        <div class="admin-topbar__search-box">
          <AdminIcon name="search" :size="16" />
          <input
            v-model="searchQ"
            type="search"
            placeholder="Tìm SP, đơn, user..."
            autocomplete="off"
            @focus="searchOpen = true"
            @input="onSearchInput"
          />
        </div>
        <div v-if="searchOpen && (searchQ.trim() || searching)" class="admin-topbar__dropdown">
          <p v-if="searching" class="admin-topbar__dropdown-empty">Đang tìm...</p>
          <template v-else-if="!hasSearchResults">
            <p class="admin-topbar__dropdown-empty">Không có kết quả</p>
          </template>
          <template v-else>
            <div v-if="searchResults.orders?.length" class="admin-topbar__group">
              <p class="admin-topbar__group-title">Đơn hàng</p>
              <button
                v-for="item in searchResults.orders"
                :key="'o-' + item.id"
                type="button"
                class="admin-topbar__hit"
                @click="goResult(item.link)"
              >
                <strong>#{{ item.id }}</strong>
                <span>{{ item.username }} · {{ item.orderStatus }}</span>
              </button>
            </div>
            <div v-if="searchResults.products?.length" class="admin-topbar__group">
              <p class="admin-topbar__group-title">Sản phẩm</p>
              <button
                v-for="item in searchResults.products"
                :key="'p-' + item.id"
                type="button"
                class="admin-topbar__hit"
                @click="goResult(item.link)"
              >
                <strong>{{ item.name }}</strong>
                <span>{{ item.brand || "—" }}</span>
              </button>
            </div>
            <div v-if="searchResults.users?.length" class="admin-topbar__group">
              <p class="admin-topbar__group-title">Người dùng</p>
              <button
                v-for="item in searchResults.users"
                :key="'u-' + item.id"
                type="button"
                class="admin-topbar__hit"
                @click="goResult(item.link)"
              >
                <strong>{{ item.username }}</strong>
                <span>{{ item.fullname || item.email || "—" }}</span>
              </button>
            </div>
            <div v-if="searchResults.categories?.length" class="admin-topbar__group">
              <p class="admin-topbar__group-title">Danh mục</p>
              <button
                v-for="item in searchResults.categories"
                :key="'c-' + item.id"
                type="button"
                class="admin-topbar__hit"
                @click="goResult(item.link)"
              >
                <strong>{{ item.name }}</strong>
              </button>
            </div>
          </template>
        </div>
      </div>

      <div class="admin-topbar__notify" ref="bellWrap">
        <button
          type="button"
          class="admin-topbar__action"
          aria-label="Thông báo"
          title="Thông báo"
          @click="toggleBell"
        >
          <AdminIcon name="bell" :size="18" />
          <span v-if="unreadCount > 0" class="admin-topbar__badge">{{ unreadCount > 99 ? "99+" : unreadCount }}</span>
        </button>
        <div v-if="bellOpen" class="admin-topbar__dropdown admin-topbar__dropdown--bell">
          <div class="admin-topbar__bell-head">
            <strong>Thông báo</strong>
            <button v-if="unreadCount > 0" type="button" class="admin-topbar__bell-all" @click="markAllRead">
              Đọc tất cả
            </button>
          </div>
          <p v-if="!notifications.length" class="admin-topbar__dropdown-empty">Chưa có thông báo</p>
          <button
            v-for="n in notifications"
            :key="n.id"
            type="button"
            class="admin-topbar__hit"
            :class="{ 'is-unread': isUnread(n) }"
            @click="openNotification(n)"
          >
            <strong>{{ n.title }}</strong>
            <span>{{ n.message }}</span>
            <small>{{ formatTime(n.createdAt) }}</small>
          </button>
        </div>
      </div>

      <RouterLink to="/product" class="admin-topbar__chip" title="Về cửa hàng">
        <AdminIcon name="home" :size="16" />
        <span>Cửa hàng</span>
      </RouterLink>
      <div class="admin-topbar__user">
        <span class="admin-topbar__user-avatar">{{ initials }}</span>
        <span class="admin-topbar__user-name">{{ username }}</span>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import LiveClock from "../LiveClock.vue";
import AdminIcon from "./AdminIcon.vue";
import { useAppStore } from "../../stores/appStore";
import {
  fetchAdminNotificationsApi,
  fetchAdminSearchApi,
  fetchAdminUnreadCountApi,
  markAdminNotificationReadApi,
  markAllAdminNotificationsReadApi,
} from "../../services/api";

defineProps({
  title: { type: String, default: "Quản trị" },
  subtitle: { type: String, default: "" },
});

defineEmits(["toggle-sidebar"]);

const router = useRouter();
const store = useAppStore();
const username = computed(() => store.state.user?.username || "admin");

const initials = computed(() => {
  const name = (store.state.user?.fullname || store.state.user?.username || "AD").trim();
  const parts = name.split(/\s+/);
  if (parts.length >= 2) return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  return name.slice(0, 2).toUpperCase();
});

const searchWrap = ref(null);
const bellWrap = ref(null);
const searchQ = ref("");
const searchOpen = ref(false);
const searching = ref(false);
const searchResults = reactive({ products: [], categories: [], users: [], orders: [] });
const bellOpen = ref(false);
const notifications = ref([]);
const unreadCount = ref(0);

let searchTimer = null;
let pollTimer = null;

const hasSearchResults = computed(
  () =>
    (searchResults.products?.length || 0) +
      (searchResults.categories?.length || 0) +
      (searchResults.users?.length || 0) +
      (searchResults.orders?.length || 0) >
    0
);

const onSearchInput = () => {
  searchOpen.value = true;
  if (searchTimer) clearTimeout(searchTimer);
  searchTimer = setTimeout(runSearch, 300);
};

const runSearch = async () => {
  const q = searchQ.value.trim();
  if (!q) {
    searchResults.products = [];
    searchResults.categories = [];
    searchResults.users = [];
    searchResults.orders = [];
    return;
  }
  searching.value = true;
  try {
    const data = await fetchAdminSearchApi(q);
    searchResults.products = data.products || [];
    searchResults.categories = data.categories || [];
    searchResults.users = data.users || [];
    searchResults.orders = data.orders || [];
  } catch {
    searchResults.products = [];
    searchResults.categories = [];
    searchResults.users = [];
    searchResults.orders = [];
  } finally {
    searching.value = false;
  }
};

const goResult = (link) => {
  searchOpen.value = false;
  searchQ.value = "";
  if (link) router.push(link);
};

const toggleBell = async () => {
  bellOpen.value = !bellOpen.value;
  searchOpen.value = false;
  if (bellOpen.value) {
    await loadNotifications();
  }
};

const loadUnread = async () => {
  try {
    const data = await fetchAdminUnreadCountApi();
    unreadCount.value = Number(data.unreadCount || 0);
  } catch {
    /* ignore */
  }
};

const loadNotifications = async () => {
  try {
    const data = await fetchAdminNotificationsApi(20);
    notifications.value = data.notifications || [];
    unreadCount.value = Number(data.unreadCount || 0);
  } catch {
    notifications.value = [];
  }
};

const isUnread = (n) => n && n.isRead !== true && n.read !== true;

const resolveNotificationTarget = (link) => {
  if (!link || typeof link !== "string") return null;
  const raw = link.trim();
  if (!raw) return null;
  try {
    // Hỗ trợ "/admin/order/1" hoặc "/admin/user?keyword=x"
    const url = new URL(raw, window.location.origin);
    return { path: url.pathname, query: Object.fromEntries(url.searchParams.entries()) };
  } catch {
    return raw.startsWith("/") ? raw : `/${raw}`;
  }
};

const openNotification = async (n) => {
  try {
    if (isUnread(n) && n.id != null) {
      await markAdminNotificationReadApi(n.id);
      n.isRead = true;
      unreadCount.value = Math.max(0, unreadCount.value - 1);
    }
  } catch {
    /* vẫn cho điều hướng dù đánh dấu đọc lỗi */
  }
  bellOpen.value = false;

  const target = resolveNotificationTarget(n?.link);
  if (!target) return;
  try {
    await router.push(target);
  } catch {
    /* bỏ qua NavigationDuplicated / hủy điều hướng */
  }
};

const markAllRead = async () => {
  try {
    await markAllAdminNotificationsReadApi();
    notifications.value = notifications.value.map((n) => ({ ...n, isRead: true }));
    unreadCount.value = 0;
  } catch {
    /* ignore */
  }
};

const formatTime = (value) => {
  if (!value) return "";
  return new Date(value).toLocaleString("vi-VN");
};

const onDocClick = (e) => {
  if (searchWrap.value && !searchWrap.value.contains(e.target)) searchOpen.value = false;
  if (bellWrap.value && !bellWrap.value.contains(e.target)) bellOpen.value = false;
};

onMounted(() => {
  document.addEventListener("click", onDocClick);
  loadUnread();
  pollTimer = setInterval(loadUnread, 20000);
});

onUnmounted(() => {
  document.removeEventListener("click", onDocClick);
  if (searchTimer) clearTimeout(searchTimer);
  if (pollTimer) clearInterval(pollTimer);
});
</script>
