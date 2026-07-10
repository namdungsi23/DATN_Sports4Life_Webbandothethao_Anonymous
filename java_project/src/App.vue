<script setup>
import { computed } from "vue";
import { RouterView, useRoute } from "vue-router";
import FloatingChatWidget from "./components/chat/FloatingChatWidget.vue";
import AdminChatPresence from "./components/chat/AdminChatPresence.vue";
import { useAppStore } from "./stores/appStore";
import { userCanAccessPanel } from "./utils/adminAccess";

const route = useRoute();
const store = useAppStore();

/** Chỉ khách hàng đã đăng nhập trên storefront — không hiện ở admin/login. */
const showFloatingChat = computed(() => {
  const path = route.path || "";
  if (!store.isLoggedIn()) return false;
  if (path.startsWith("/admin")) return false;
  if (path === "/login" || path === "/register") return false;
  if (userCanAccessPanel(store.state.user)) return false;
  return true;
});

/** Employee/Admin vào panel → giữ WS online để nhận assignment. */
const showAdminPresence = computed(() => {
  const path = route.path || "";
  return path.startsWith("/admin") && userCanAccessPanel(store.state.user);
});
</script>

<template>
  <RouterView />
  <FloatingChatWidget v-if="showFloatingChat" />
  <AdminChatPresence v-if="showAdminPresence" />
</template>
