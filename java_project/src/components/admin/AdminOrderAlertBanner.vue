<template>
  <div
    v-if="visible"
    class="alert alert-warning border-warning mb-0 rounded-0 d-flex flex-wrap align-items-center justify-content-between gap-2 admin-order-alert"
  >
    <div>
      <strong>{{ pendingCount }} đơn hàng mới</strong>
      <span class="ms-1">đang chờ xác nhận.</span>
      <span v-if="recentPending.length" class="ms-2 small text-muted">
        (Mới nhất: #{{ recentPending.map((o) => o.id).join(", #") }})
      </span>
    </div>
    <div class="d-flex gap-2">
      <RouterLink to="/admin/order" class="btn btn-sm btn-warning">Xác nhận ngay</RouterLink>
      <button type="button" class="btn btn-sm btn-outline-secondary" aria-label="Đóng" @click="dismiss">
        ✕
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { RouterLink } from "vue-router";
import { apiFetch } from "../../services/http.js";
import { useAppStore } from "../../stores/appStore";
import { ADMIN_PERMS, userHasPermission } from "../../utils/adminAccess";

const POLL_MS = 20_000;

const store = useAppStore();
const pendingCount = ref(0);
const recentPending = ref([]);
const dismissed = ref(false);
const lastSeenCount = ref(0);
let pollTimer = null;

const visible = computed(() => {
  if (dismissed.value || pendingCount.value <= 0) return false;
  return userHasPermission(store.state.user, ADMIN_PERMS.ORDER);
});

function dismiss() {
  dismissed.value = true;
  lastSeenCount.value = pendingCount.value;
}

async function loadAlerts() {
  if (!userHasPermission(store.state.user, ADMIN_PERMS.ORDER)) return;
  try {
    const data = await apiFetch("/api/admin/orders/alerts");
    const count = Number(data.pendingCount || 0);
    recentPending.value = data.recentPending || [];

    if (count > lastSeenCount.value && dismissed.value) {
      dismissed.value = false;
    }
    if (count === 0) {
      dismissed.value = false;
      lastSeenCount.value = 0;
    }

    pendingCount.value = count;
  } catch {
    pendingCount.value = 0;
    recentPending.value = [];
  }
}

function startPolling() {
  stopPolling();
  if (!userHasPermission(store.state.user, ADMIN_PERMS.ORDER)) return;
  pollTimer = window.setInterval(() => {
    if (document.visibilityState === "visible") {
      loadAlerts();
    }
  }, POLL_MS);
}

function stopPolling() {
  if (pollTimer != null) {
    window.clearInterval(pollTimer);
    pollTimer = null;
  }
}

function onVisibilityChange() {
  if (document.visibilityState === "visible") {
    loadAlerts();
  }
}

onMounted(() => {
  loadAlerts();
  startPolling();
  document.addEventListener("visibilitychange", onVisibilityChange);
});

onUnmounted(() => {
  stopPolling();
  document.removeEventListener("visibilitychange", onVisibilityChange);
});

watch(
  () => store.state.user,
  () => {
    loadAlerts();
    startPolling();
  },
  { deep: true }
);

defineExpose({ reload: loadAlerts });
</script>
