<template>
  <MainLayout>
    <div class="checkout-page">
      <div class="checkout-success" :class="`checkout-success--${statusType}`">
        <div class="checkout-success__icon">{{ icon }}</div>
        <h2>{{ title }}</h2>
        <p class="text-muted mb-3">{{ message }}</p>
        <div v-if="orderId" class="checkout-success__order mb-3">
          Mã đơn hàng: <strong>#{{ orderId }}</strong>
        </div>
        <p v-if="statusType === 'success' && !paid && polling" class="checkout-summary__hint">
          Đang xác nhận thanh toán từ SePay... ({{ pollCount }}/{{ maxPoll }})
        </p>
        <p v-if="statusType === 'success' && !paid && !polling" class="checkout-summary__hint text-warning">
          SePay chưa gửi xác nhận. Nếu bạn đã quét QR và chuyển khoản, hãy bấm «Kiểm tra lại» hoặc xem đơn hàng sau vài phút.
        </p>
        <button
          v-if="statusType === 'success' && !paid"
          type="button"
          class="checkout-btn checkout-btn--outline mb-2"
          style="max-width: 280px; margin: 0 auto"
          :disabled="checking"
          @click="checkNow"
        >
          {{ checking ? "Đang kiểm tra..." : "Kiểm tra lại" }}
        </button>
        <p v-if="paid" class="text-success fw-semibold">Thanh toán đã được xác nhận thành công.</p>
        <div class="d-flex flex-column gap-2" style="max-width: 280px; margin: 0 auto">
          <RouterLink to="/profile" class="checkout-btn checkout-btn--primary">Xem đơn hàng</RouterLink>
          <RouterLink to="/product" class="checkout-btn checkout-btn--outline">Tiếp tục mua sắm</RouterLink>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { RouterLink, useRoute } from "vue-router";
import MainLayout from "../../layouts/MainLayout.vue";
import { fetchSePayStatusApi } from "../../services/api";

const route = useRoute();
const orderId = computed(() => route.query.orderId || null);
const statusType = computed(() => {
  if (route.path.includes("/success")) return "success";
  if (route.path.includes("/error")) return "error";
  return "cancel";
});

const paid = ref(false);
const pollCount = ref(0);
const polling = ref(true);
const checking = ref(false);
const maxPoll = 30;
let timer = null;

const icon = computed(() => {
  if (statusType.value === "success") return paid.value ? "✓" : "⏳";
  if (statusType.value === "error") return "✕";
  return "↩";
});

const title = computed(() => {
  if (statusType.value === "success") return paid.value ? "Thanh toán thành công!" : "Đang xử lý thanh toán";
  if (statusType.value === "error") return "Thanh toán thất bại";
  return "Đã hủy thanh toán";
});

const message = computed(() => {
  if (statusType.value === "success") {
    return paid.value
      ? "Cảm ơn bạn! Đơn hàng đã được thanh toán qua SePay."
      : "SePay đang xác nhận giao dịch. Vui lòng đợi trong giây lát.";
  }
  if (statusType.value === "error") {
    return "Giao dịch không thành công. Bạn có thể thử lại hoặc chọn phương thức khác.";
  }
  return "Bạn đã hủy thanh toán SePay. Đơn hàng vẫn được lưu ở trạng thái chưa thanh toán.";
});

async function pollStatus() {
  if (!orderId.value || statusType.value !== "success" || paid.value) {
    return false;
  }
  if (pollCount.value >= maxPoll) {
    polling.value = false;
    clearTimer();
    return false;
  }
  pollCount.value += 1;
  try {
    const data = await fetchSePayStatusApi(orderId.value);
    if (data?.paid) {
      paid.value = true;
      polling.value = false;
      clearTimer();
      return true;
    }
  } catch {
    /* ignore polling errors */
  }
  if (pollCount.value >= maxPoll) {
    polling.value = false;
    clearTimer();
  }
  return false;
}

async function checkNow() {
  if (!orderId.value || paid.value) return;
  checking.value = true;
  try {
    const data = await fetchSePayStatusApi(orderId.value);
    if (data?.paid) {
      paid.value = true;
      polling.value = false;
      clearTimer();
    }
  } catch {
    /* ignore */
  } finally {
    checking.value = false;
  }
}

function clearTimer() {
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
}

onMounted(() => {
  if (statusType.value === "success" && orderId.value) {
    pollStatus();
    timer = setInterval(pollStatus, 2500);
  } else {
    polling.value = false;
  }
});

onUnmounted(clearTimer);
</script>

<style scoped>
.checkout-success--error .checkout-success__icon {
  background: #ffebee;
  color: #c62828;
}

.checkout-success--cancel .checkout-success__icon {
  background: #fff8e1;
  color: #f57c00;
}
</style>
