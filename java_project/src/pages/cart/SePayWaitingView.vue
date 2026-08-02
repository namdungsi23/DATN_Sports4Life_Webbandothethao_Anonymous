<template>
  <MainLayout>
    <div class="checkout-page">
      <div v-if="paid" class="checkout-success">
        <div class="checkout-success__icon">✓</div>
        <h2>Thanh toán thành công!</h2>
        <p class="text-muted mb-3">
          Tiền đã được ghi nhận. Shop sẽ xác nhận đơn hàng và liên hệ giao hàng sớm.
        </p>
        <div v-if="orderId" class="checkout-success__order mb-3">
          Mã đơn hàng: <strong>#{{ orderId }}</strong>
        </div>
        <div class="d-flex flex-column gap-2" style="max-width: 280px; margin: 0 auto">
          <RouterLink to="/profile?tab=orders" class="checkout-btn checkout-btn--primary">
            Xem đơn hàng
          </RouterLink>
          <RouterLink to="/product" class="checkout-btn checkout-btn--outline">
            Tiếp tục mua sắm
          </RouterLink>
        </div>
      </div>

      <div v-else-if="finishing" class="checkout-success">
        <div class="checkout-success__icon">✓</div>
        <h2>SePay đã nhận tiền</h2>
        <p class="text-muted mb-3">Đang cập nhật trạng thái thanh toán...</p>
      </div>

      <div v-else class="checkout-card" style="max-width: 520px; margin: 0 auto">
        <div class="checkout-card__head">
          <span class="checkout-card__head-icon">📱</span>
          Quét mã QR chuyển khoản
        </div>
        <div class="checkout-card__body text-center">
          <div v-if="orderId" class="checkout-success__order mb-3">
            Mã đơn: <strong>#{{ orderId }}</strong>
            <span v-if="displayAmount"> · {{ displayAmount }}đ</span>
          </div>
          <p class="mb-2">
            Tab SePay đã mở — quét mã QR và chuyển khoản đúng số tiền.
          </p>
          <p class="checkout-summary__hint mb-3">
            Sau khi SePay báo thành công, hệ thống sẽ ghi nhận thanh toán. Admin sẽ xác nhận đơn thủ công.
          </p>
          <button
            type="button"
            class="checkout-btn checkout-btn--outline"
            style="max-width: 280px; margin: 0 auto"
            @click="openSePayTab"
          >
            Mở lại trang QR SePay
          </button>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { RouterLink, useRoute } from "vue-router";
import MainLayout from "../../layouts/MainLayout.vue";
import { completeSePayPaymentApi, ensureAuthSession } from "../../services/api";
import { loadSePayCheckout, loadSePayCompletionToken, openSePayCheckoutTab } from "../../utils/sepay";

const SEPAY_STORAGE_KEY = "sepay_checkout_pending";

const route = useRoute();
const orderId = computed(() => route.query.orderId || null);
const fromGateway = computed(() => route.query.gateway === "success");

const paid = ref(false);
const finishing = ref(false);
const displayAmount = ref("");
let sepayPayload = null;
let completionToken = null;

const formatPrice = (price) => Number(price || 0).toLocaleString("vi-VN");

async function finalizeOrder() {
  if (!orderId.value || paid.value) return true;
  try {
    await ensureAuthSession({ forceRefresh: true });
    const data = await completeSePayPaymentApi(orderId.value, {
      gateway: fromGateway.value,
      completionToken,
    });
    if (data?.totalAmount != null) {
      displayAmount.value = formatPrice(data.totalAmount);
    }
    if (data?.paid) {
      paid.value = true;
      finishing.value = false;
      sessionStorage.removeItem(SEPAY_STORAGE_KEY);
      return true;
    }
  } catch {
    /* retry */
  }
  return false;
}

function openSePayTab() {
  if (sepayPayload) {
    openSePayCheckoutTab(sepayPayload);
  }
}

async function onReturnFromSePay() {
  if (paid.value || fromGateway.value) return;
  finishing.value = true;
  await ensureAuthSession({ forceRefresh: true });
  const ok = await finalizeOrder();
  if (!ok) {
    await new Promise((r) => setTimeout(r, 1200));
    await finalizeOrder();
  }
  if (!paid.value) {
    finishing.value = false;
  }
}

function onVisibilityChange() {
  if (document.visibilityState === "visible") {
    onReturnFromSePay();
  }
}

function onWindowFocus() {
  onReturnFromSePay();
}

onMounted(async () => {
  if (!orderId.value) return;

  sepayPayload = loadSePayCheckout(SEPAY_STORAGE_KEY);
  completionToken = loadSePayCompletionToken(SEPAY_STORAGE_KEY);

  await ensureAuthSession({ forceRefresh: true });

  if (fromGateway.value) {
    finishing.value = true;
    let ok = await finalizeOrder();
    if (!ok) {
      await new Promise((r) => setTimeout(r, 1500));
      ok = await finalizeOrder();
    }
    if (!ok) {
      finishing.value = false;
    }
    return;
  }

  if (sepayPayload) {
    openSePayCheckoutTab(sepayPayload);
  }

  window.addEventListener("focus", onWindowFocus);
  document.addEventListener("visibilitychange", onVisibilityChange);
});

onUnmounted(() => {
  window.removeEventListener("focus", onWindowFocus);
  document.removeEventListener("visibilitychange", onVisibilityChange);
});
</script>
