<template>
  <MainLayout>
    <div class="checkout-page">
      <div class="checkout-success" :class="`checkout-success--${statusType}`">
        <div class="checkout-success__icon">{{ icon }}</div>
        <h2>{{ title }}</h2>
        <p class="text-muted mb-3">{{ message }}</p>
        <div v-if="orderId" class="checkout-success__order mb-3">
          Mã đơn hàng: <strong>#{{ orderId }}</strong>
          <span v-if="invoiceNumber" class="d-block small text-muted mt-1">Mã SePay: {{ invoiceNumber }}</span>
        </div>
        <p v-if="statusType === 'success' && !paid && syncHint" class="checkout-summary__hint text-warning">
          {{ syncHint }}
        </p>
        <p v-if="statusType === 'success' && !paid" class="checkout-summary__hint">
          Đang hỏi SePay... ({{ pollCount }}/{{ maxPoll }})
        </p>
        <p v-if="statusType === 'success' && paid" class="text-success fw-semibold">
          SePay đã xác nhận thanh toán.
        </p>
        <div v-if="statusType === 'success' && !paid && !checking" class="d-flex flex-column gap-2 mb-3" style="max-width: 320px; margin: 0 auto">
          <button type="button" class="checkout-btn checkout-btn--primary" @click="manualCheck">
            Kiểm tra lại thanh toán
          </button>
          <p class="small text-muted mb-0">Đã chuyển khoản? Bấm nút này — hệ thống hỏi SePay trực tiếp.</p>
        </div>
        <div v-if="statusType === 'cancel' && orderId" class="d-flex flex-column gap-2 mb-3" style="max-width: 320px; margin: 0 auto">
          <RouterLink :to="`/profile?tab=orders&orderId=${orderId}`" class="checkout-btn checkout-btn--primary">
            Thanh toán lại đơn này
          </RouterLink>
        </div>
        <div v-if="!showSuccessModal" class="d-flex flex-column gap-2" style="max-width: 280px; margin: 0 auto">
          <RouterLink to="/profile?tab=orders" class="checkout-btn checkout-btn--outline">Xem đơn hàng</RouterLink>
          <RouterLink to="/product" class="checkout-btn checkout-btn--outline">Tiếp tục mua sắm</RouterLink>
        </div>
      </div>
    </div>

    <div v-if="showSuccessModal" class="payment-success-modal" role="dialog" aria-modal="true">
      <div class="payment-success-modal__backdrop" />
      <div class="payment-success-modal__card">
        <div class="payment-success-modal__icon">✓</div>
        <h3>Thanh toán hoàn tất!</h3>
        <p class="payment-success-modal__amount">
          Số tiền đã thanh toán: <strong>{{ formatPrice(paidAmount) }}đ</strong>
        </p>
        <p v-if="invoiceCode" class="payment-success-modal__meta">
          Hóa đơn: <strong>{{ invoiceCode }}</strong>
        </p>
        <button type="button" class="checkout-btn checkout-btn--primary w-100" @click="goToInvoice">
          OK — Xem hóa đơn
        </button>
      </div>
    </div>
  </MainLayout>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import MainLayout from "../../layouts/MainLayout.vue";
import { fetchSePayStatusApi, syncSePayPaymentApi } from "../../services/api";

const route = useRoute();
const router = useRouter();
const orderId = computed(() => route.query.orderId || null);
const statusType = computed(() => {
  if (route.path.includes("/success")) return "success";
  if (route.path.includes("/error")) return "error";
  return "cancel";
});

const paid = ref(false);
const pollCount = ref(0);
const maxPoll = 40;
const paidAmount = ref(0);
const invoiceCode = ref("");
const invoiceNumber = ref("");
const syncHint = ref("");
const checking = ref(false);
const showSuccessModal = ref(false);
let timer = null;

const formatPrice = (price) => Number(price || 0).toLocaleString("vi-VN");

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
      ? "Cảm ơn bạn! Giao dịch đã được SePay xác nhận."
      : "Nếu bạn đã quét QR và chuyển khoản, hệ thống sẽ tự kiểm tra với SePay.";
  }
  if (statusType.value === "error") {
    return "Giao dịch không thành công. Vào Profile → Đơn hàng để thử lại.";
  }
  return "Đơn vẫn được lưu — bạn có thể thanh toán lại trong trang cá nhân.";
});

function applyStatus(data) {
  if (!data) return;
  invoiceNumber.value = data.invoiceNumber || "";
  if (data.paid || data.paymentComplete) {
    paid.value = true;
    paidAmount.value = data.amountPaid || data.totalAmount || data.amountRequired || 0;
    invoiceCode.value = data.invoiceCode || "";
    showSuccessModal.value = true;
    clearTimer();
    return;
  }
  const sync = data.sepaySync;
  if (sync?.message) {
    syncHint.value = sync.message;
  }
}

async function pollStatus() {
  if (!orderId.value || statusType.value !== "success" || paid.value || pollCount.value >= maxPoll) {
    if (pollCount.value >= maxPoll && !paid.value) {
      clearTimer();
      syncHint.value = syncHint.value || "Chưa thấy xác nhận từ SePay. Bấm 'Kiểm tra lại thanh toán'.";
    }
    return;
  }
  pollCount.value += 1;
  try {
    const data = await fetchSePayStatusApi(orderId.value);
    applyStatus(data);
  } catch {
    /* ignore */
  }
}

async function manualCheck() {
  if (!orderId.value || checking.value) return;
  checking.value = true;
  syncHint.value = "Đang hỏi SePay...";
  try {
    const data = await syncSePayPaymentApi(orderId.value);
    applyStatus(data);
    if (!paid.value && data?.sepaySync?.message) {
      syncHint.value = data.sepaySync.message;
    } else if (!paid.value && data?.message) {
      syncHint.value = data.message;
    }
  } catch (err) {
    syncHint.value = err?.response?.data?.message || "Không kiểm tra được. Thử lại sau.";
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

function goToInvoice() {
  router.push({
    path: "/profile",
    query: { tab: "orders", orderId: String(orderId.value), invoice: "1" },
  });
}

onMounted(() => {
  if (statusType.value === "success" && orderId.value) {
    pollStatus();
    timer = setInterval(pollStatus, 3000);
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

.payment-success-modal {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.payment-success-modal__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
}

.payment-success-modal__card {
  position: relative;
  width: min(100%, 420px);
  background: #fff;
  border-radius: 16px;
  padding: 28px 24px;
  text-align: center;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.18);
}

.payment-success-modal__icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  border-radius: 50%;
  background: #dcfce7;
  color: #166534;
  font-size: 32px;
  line-height: 64px;
}

.payment-success-modal__amount {
  font-size: 18px;
  margin: 12px 0;
}

.payment-success-modal__meta {
  color: #555;
  margin-bottom: 20px;
}
</style>
