<template>
  <AdminLayout>
    <div class="admin-dashboard__intro">
      <p>Tổng quan hoạt động cửa hàng Sports4Life.</p>
    </div>

    <div v-if="err" class="alert alert-danger">{{ err }}</div>

    <div class="admin-stat-grid">
      <div v-for="card in statCards" :key="card.label" class="admin-stat-card">
        <div class="admin-stat-card__icon" :class="`admin-stat-card__icon--${card.tone}`">
          <AdminIcon :name="card.icon" :size="24" />
        </div>
        <div>
          <p class="admin-stat-card__value">{{ card.value }}</p>
          <p class="admin-stat-card__label">{{ card.label }}</p>
        </div>
      </div>
    </div>

    <div class="admin-panel">
      <div class="admin-panel__head">
        <h2>
          <AdminIcon name="settings" :size="18" />
          Truy cập nhanh
        </h2>
      </div>
      <div class="admin-panel__body">
        <div class="admin-quick-links">
          <RouterLink
            v-for="link in quickLinks"
            :key="link.to"
            :to="link.to"
            class="admin-quick-link"
          >
            <span class="admin-quick-link__icon">
              <AdminIcon :name="link.icon" :size="20" />
            </span>
            <span>{{ link.label }}</span>
          </RouterLink>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import AdminLayout from "../../layouts/AdminLayout.vue";
import AdminIcon from "../../components/admin/AdminIcon.vue";
import { filterAdminMenu } from "../../utils/adminAccess";
import { useAppStore } from "../../stores/appStore";
import { apiFetch } from "../../services/http.js";

const store = useAppStore();

const err = ref("");
const stats = ref({
  totalUsers: 0,
  totalProducts: 0,
  totalOrders: 0,
  todayOrders: 0,
  newProducts: 0,
});

const statCards = computed(() => [
  { icon: "users", label: "Tài khoản", value: stats.value.totalUsers, tone: "blue" },
  { icon: "product", label: "Sản phẩm", value: stats.value.totalProducts, tone: "green" },
  { icon: "order", label: "Đơn hàng", value: stats.value.totalOrders, tone: "orange" },
  { icon: "chart", label: "Đơn hôm nay", value: stats.value.todayOrders, tone: "purple" },
  { icon: "package", label: "SP mới (7 ngày)", value: stats.value.newProducts, tone: "red" },
]);

const quickLinks = computed(() =>
  filterAdminMenu(store.state.user).map((item) => ({
    to: item.to,
    label: `Quản lý ${item.label.toLowerCase()}`,
    icon: item.icon,
  }))
);

onMounted(async () => {
  try {
    const data = await apiFetch("/api/admin/dashboard");
    stats.value = { ...stats.value, ...data };
  } catch (e) {
    err.value = e?.message || "Không tải được thống kê dashboard.";
  }
});
</script>
