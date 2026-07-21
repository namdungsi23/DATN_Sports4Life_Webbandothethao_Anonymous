<template>
  <aside class="admin-sidebar" :class="{ 'is-collapsed': collapsed, 'is-mobile-open': mobileOpen }">
    <div class="admin-sidebar__brand">
      <RouterLink to="/admin/dashboard" class="admin-sidebar__logo" @click="closeMobile">
        <span class="admin-sidebar__logo-mark">
          <AdminIcon name="dashboard" :size="20" />
        </span>
        <span class="admin-sidebar__logo-text">Sports4Life</span>
      </RouterLink>
      <button
        type="button"
        class="admin-sidebar__close"
        aria-label="Đóng menu"
        @click="$emit('close-mobile')"
      >
        <AdminIcon name="close" :size="18" />
      </button>
    </div>

    <nav class="admin-sidebar__nav">
      <p class="admin-sidebar__section">Menu</p>
      <RouterLink
        v-for="item in menuItems"
        :key="item.to"
        :to="item.to"
        class="admin-sidebar__link"
        active-class="is-active"
        :title="collapsed ? item.label : ''"
        @click="closeMobile"
      >
        <span class="admin-sidebar__icon-wrap" :class="`admin-sidebar__icon-wrap--${item.tone}`">
          <AdminIcon :name="item.icon" :size="18" :tone="item.tone" />
        </span>
        <span class="admin-sidebar__label">{{ item.label }}</span>
      </RouterLink>

      <p class="admin-sidebar__section">Hệ thống</p>
      <RouterLink
        to="/product"
        class="admin-sidebar__link"
        :title="collapsed ? 'Về cửa hàng' : ''"
        @click="closeMobile"
      >
        <span class="admin-sidebar__icon-wrap admin-sidebar__icon-wrap--green">
          <AdminIcon name="store" :size="18" tone="green" />
        </span>
        <span class="admin-sidebar__label">Về cửa hàng</span>
      </RouterLink>
      <button
        type="button"
        class="admin-sidebar__link admin-sidebar__link--btn"
        :title="collapsed ? 'Đăng xuất' : ''"
        @click="onLogout"
      >
        <span class="admin-sidebar__icon-wrap admin-sidebar__icon-wrap--red">
          <AdminIcon name="logout" :size="18" tone="red" />
        </span>
        <span class="admin-sidebar__label">Đăng xuất</span>
      </button>
    </nav>

    <div class="admin-sidebar__footer">
      <div class="admin-sidebar__user">
        <span class="admin-sidebar__avatar">{{ initials }}</span>
        <div class="admin-sidebar__user-text">
          <strong>{{ displayName }}</strong>
          <small>{{ roleLabel }}</small>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { computed } from "vue";
import { RouterLink, useRouter } from "vue-router";
import AdminIcon from "./AdminIcon.vue";
import { filterAdminMenu, panelRoleLabel } from "../../utils/adminAccess";
import { useAppStore } from "../../stores/appStore";

defineProps({
  collapsed: { type: Boolean, default: false },
  mobileOpen: { type: Boolean, default: false },
});

const emit = defineEmits(["close-mobile"]);

const router = useRouter();
const store = useAppStore();

const menuItems = computed(() => filterAdminMenu(store.state.user));

const displayName = computed(
  () => store.state.user?.fullname || store.state.user?.username || "Admin"
);

const roleLabel = computed(() => panelRoleLabel(store.state.user));

const initials = computed(() => {
  const name = displayName.value.trim();
  const parts = name.split(/\s+/);
  if (parts.length >= 2) return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  return name.slice(0, 2).toUpperCase();
});

const closeMobile = () => emit("close-mobile");

const onLogout = () => {
  closeMobile();
  store.logout();
  router.push("/login");
};
</script>
