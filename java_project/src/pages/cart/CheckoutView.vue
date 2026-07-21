<template>
  <MainLayout>
    <div class="checkout-page">
      <div class="checkout-page__head">
        <h1>Thanh toán</h1>
        <p>Chọn phương thức thanh toán và xem lại đơn hàng</p>
      </div>

      <CheckoutSteps current="payment" />

      <div v-if="error" class="checkout-alert checkout-alert--error">{{ error }}</div>

      <div class="checkout-layout">
        <div class="checkout-main">
          <div class="checkout-card">
            <div class="checkout-card__head">
              <span class="checkout-card__head-icon">🛍️</span>
              Sản phẩm ({{ cartItems.length }})
            </div>
            <div class="checkout-card__body">
              <div class="checkout-items">
                <div v-for="item in cartItems" :key="item.variantId || item.productId" class="checkout-item">
                  <div class="checkout-item__img">
                    <ProductImage :src="item.image" :alt="item.name" />
                  </div>
                  <div>
                    <p class="checkout-item__name">{{ item.name }}</p>
                    <p v-if="item.size || item.color" class="checkout-item__variant">
                      {{ [item.color, item.size ? `Size ${item.size}` : null].filter(Boolean).join(" · ") }}
                    </p>
                    <p class="checkout-item__price">{{ formatPrice(item.price) }}đ × {{ item.quantity }}</p>
                  </div>
                  <div class="checkout-item__subtotal">{{ formatPrice(item.price * item.quantity) }}đ</div>
                </div>
              </div>
            </div>
          </div>

          <form class="checkout-card" @submit.prevent="submitPayment">
            <div class="checkout-card__head">
              <span class="checkout-card__head-icon">💳</span>
              Phương thức thanh toán
            </div>
            <div class="checkout-card__body">
              <div class="pay-methods">
                <label
                  v-for="method in methods"
                  :key="method.value"
                  class="pay-method"
                  :class="{ 'pay-method--active': paymentMethod === method.value }"
                >
                  <input v-model="paymentMethod" type="radio" :value="method.value" />
                  <span class="pay-method__check">✓</span>
                  <span class="pay-method__icon">{{ method.icon }}</span>
                  <span class="pay-method__title">{{ method.label }}</span>
                  <span class="pay-method__desc">{{ method.desc }}</span>
                </label>
              </div>
            </div>
          </form>
        </div>

        <aside class="checkout-summary">
          <div class="checkout-summary__head">
            <h2>Tóm tắt đơn hàng</h2>
          </div>
          <div class="checkout-summary__body">
            <div class="checkout-summary__row">
              <span>Tạm tính</span>
              <span>{{ formatPrice(amount) }}đ</span>
            </div>
            <div class="checkout-summary__row">
              <span>Phí vận chuyển</span>
              <span :class="{ 'checkout-summary__row--discount': !shippingFee }">
                {{ shippingFee ? formatPrice(shippingFee) + "đ" : "Miễn phí" }}
              </span>
            </div>
            <div v-if="amount < FREE_SHIP_THRESHOLD" class="checkout-summary__hint">
              Mua thêm <strong>{{ formatPrice(FREE_SHIP_THRESHOLD - amount) }}đ</strong> để được freeship!
            </div>
            <div class="checkout-summary__total">
              <span>Tổng cộng</span>
              <strong>{{ formatPrice(total) }}đ</strong>
            </div>
            <button
              type="button"
              class="checkout-btn checkout-btn--primary"
              :disabled="!cartItems.length"
              @click="submitPayment"
            >
              Tiếp tục — Giao hàng
            </button>
            <RouterLink to="/cart" class="checkout-btn checkout-btn--outline" style="margin-top: 10px">
              ← Quay lại giỏ hàng
            </RouterLink>
            <div class="checkout-summary__secure">
              🔒 Thanh toán an toàn · Bảo mật SSL
            </div>
          </div>
        </aside>
      </div>
    </div>
  </MainLayout>
</template>

<script setup>
import { computed, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import MainLayout from "../../layouts/MainLayout.vue";
import ProductImage from "../../components/ProductImage.vue";
import CheckoutSteps from "../../components/CheckoutSteps.vue";
import { useAppStore } from "../../stores/appStore";
import { calcShippingFee, calcOrderTotal, FREE_SHIP_THRESHOLD } from "../../utils/shipping";

const router = useRouter();
const store = useAppStore();
const cartItems = computed(() => store.state.cartItems);
const paymentMethod = ref("CASH");
const error = ref("");

const methods = [
  { value: "CASH", label: "Thanh toán khi nhận hàng", desc: "Kiểm tra hàng trước khi trả tiền", icon: "🚚" },
  { value: "SEPAY", label: "SePay (QR / Thẻ)", desc: "Quét mã QR ngân hàng, thẻ quốc tế", icon: "💳" },
  { value: "MOMO", label: "Ví MoMo", desc: "Quét mã hoặc chuyển khoản nhanh", icon: "📱" },
  { value: "TECHCOMBANK", label: "Techcombank", desc: "Chuyển khoản ngân hàng", icon: "🏦" },
];

const amount = computed(() => store.cartAmount.value);
const shippingFee = computed(() => calcShippingFee(amount.value));
const total = computed(() => calcOrderTotal(amount.value));
const formatPrice = (price) => Number(price || 0).toLocaleString("vi-VN");

const submitPayment = () => {
  if (!cartItems.value.length) {
    error.value = "Giỏ hàng đang trống.";
    return;
  }
  router.push({
    path: "/cart/payment",
    query: { method: paymentMethod.value, amount: String(total.value) },
  });
};
</script>
