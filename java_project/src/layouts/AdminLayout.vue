<template>
  <div class="admin-shell" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
    <div
      v-if="mobileSidebarOpen"
      class="admin-shell__backdrop"
      @click="mobileSidebarOpen = false"
    />

    <AdminSidebar
      :collapsed="sidebarCollapsed"
      :mobile-open="mobileSidebarOpen"
      @close-mobile="mobileSidebarOpen = false"
    />

    <div class="admin-shell__main">
      <AdminTopBar
        :title="pageTitle"
        :subtitle="pageSubtitle"
        @toggle-sidebar="toggleSidebar"
      />

      <AdminOrderAlertBanner />

      <main class="admin-content">
        <slot />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import AdminSidebar from "../components/admin/AdminSidebar.vue";
import AdminTopBar from "../components/admin/AdminTopBar.vue";
import AdminOrderAlertBanner from "../components/admin/AdminOrderAlertBanner.vue";
import { fetchAdminMeApi } from "../services/api";
import { useAppStore } from "../stores/appStore";
import { userCanAccessPanel } from "../utils/adminAccess";

const route = useRoute();
const store = useAppStore();
const sidebarCollapsed = ref(false);
const mobileSidebarOpen = ref(false);

const pageTitle = computed(() => route.meta.pageTitle || "Quản trị");
const pageSubtitle = computed(() => route.meta.pageSubtitle || "");

const toggleSidebar = () => {
  if (window.innerWidth <= 992) {
    mobileSidebarOpen.value = !mobileSidebarOpen.value;
    return;
  }
  sidebarCollapsed.value = !sidebarCollapsed.value;
};

onMounted(async () => {
  if (!userCanAccessPanel(store.state.user)) return;
  try {
    const data = await fetchAdminMeApi();
    store.syncPanelAccess(data);
  } catch {
    /* optional */
  }
});
</script>

<style>
@import url("https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css");
</style>
