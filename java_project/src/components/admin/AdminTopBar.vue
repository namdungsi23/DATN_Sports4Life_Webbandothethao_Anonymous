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
      <button type="button" class="admin-topbar__action" aria-label="Tìm kiếm" title="Tìm kiếm">
        <AdminIcon name="search" :size="18" />
      </button>
      <button type="button" class="admin-topbar__action" aria-label="Thông báo" title="Thông báo">
        <AdminIcon name="bell" :size="18" />
      </button>
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
import { computed } from "vue";
import { RouterLink } from "vue-router";
import AdminIcon from "./AdminIcon.vue";
import { useAppStore } from "../../stores/appStore";

defineProps({
  title: { type: String, default: "Quản trị" },
  subtitle: { type: String, default: "" },
});

defineEmits(["toggle-sidebar"]);

const store = useAppStore();
const username = computed(() => store.state.user?.username || "admin");

const initials = computed(() => {
  const name = (store.state.user?.fullname || store.state.user?.username || "AD").trim();
  const parts = name.split(/\s+/);
  if (parts.length >= 2) return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  return name.slice(0, 2).toUpperCase();
});
</script>
