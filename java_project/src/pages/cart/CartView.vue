<template>
  <MainLayout>
    <div class="cart-page">
      <div class="cart-page__head">
        <h1>Giỏ hàng</h1>
        <p>{{ cartItems.length ? `${cartCount} sản phẩm` : "Chưa có sản phẩm nào" }}</p>
      </div>

      <div v-if="!cartItems.length" class="cart-empty">
        <div class="cart-empty__icon">🛒</div>
        <h2>Giỏ hàng trống</h2>
        <p>Hãy khám phá bộ sưu tập giày thể thao chính hãng và thêm sản phẩm yêu thích.</p>
        <RouterLink to="/product" class="cart-empty__btn">Tiếp tục mua sắm</RouterLink>
      </div>

      <div v-else class="cart-layout">
        <section class="cart-list">
          <div v-for="item in cartItems" :key="`${item.productId}-${item.variantId ?? 'x'}`" class="cart-item">
            <div class="cart-item__img">
              <ProductImage :src="item.image" :alt="item.name" />
            </div>
            <div class="cart-item__info">
              <h3>{{ item.name }}</h3>
              <p v-if="item.color || item.size" class="cart-item__meta">
                <span v-if="item.color">Màu: {{ item.color }}</span>
                <span v-if="item.size">Size: {{ item.size }}</span>
              </p>
              <p class="cart-item__price">{{ formatPrice(item.price) }}đ</p>
              <div class="cart-item__actions">
                <div class="cart-qty">
                  <button type="button" aria-label="Giảm" @click="changeQty(item, item.quantity - 1)">−</button>
                  <span>{{ item.quantity }}</span>
                  <button type="button" aria-label="Tăng" @click="changeQty(item, item.quantity + 1)">+</button>
                </div>
                <button type="button" class="cart-item__remove" @click="removeItem(item)">Xóa</button>
              </div>
            </div>
            <div class="cart-item__subtotal">
              <span>Tạm tính</span>
              <strong>{{ formatPrice(item.price * item.quantity) }}đ</strong>
            </div>
          </div>
        </section>

        <aside class="cart-summary">
          <h2>Tóm tắt đơn hàng</h2>
          <div class="cart-summary__row">
            <span>Tạm tính ({{ cartCount }} sp)</span>
            <span>{{ formatPrice(amount) }}đ</span>
          </div>
          <div class="cart-summary__row">
            <span>Phí vận chuyển</span>
            <span class="cart-summary__free">{{ shippingFee ? formatPrice(shippingFee) + "đ" : "Miễn phí" }}</span>
          </div>
          <div v-if="amount < 499000" class="cart-summary__hint">
            Mua thêm <strong>{{ formatPrice(499000 - amount) }}đ</strong> để được freeship!
          </div>
          <div class="cart-summary__total">
            <span>Tổng cộng</span>
            <strong>{{ formatPrice(total) }}đ</strong>
          </div>
          <RouterLink to="/cart/checkout" class="cart-summary__checkout">Tiến hành thanh toán</RouterLink>
          <RouterLink to="/product" class="cart-summary__continue">← Tiếp tục mua sắm</RouterLink>
          <ul class="cart-summary__benefits">
            <li>✓ Đổi trả trong 7 ngày</li>
            <li>✓ Thanh toán COD / VNPAY / Momo</li>
            <li>✓ Hỗ trợ 24/7: 0336 694 988</li>
          </ul>
        </aside>
      </div>
    </div>
  </MainLayout>
</template>

<script setup>
import { computed } from "vue";
import { RouterLink } from "vue-router";
import MainLayout from "../../layouts/MainLayout.vue";
import ProductImage from "../../components/ProductImage.vue";
import { useAppStore } from "../../stores/appStore";

const store = useAppStore();
const cartItems = computed(() => store.state.cartItems);
const cartCount = computed(() => store.cartCount.value);
const amount = computed(() => store.cartAmount.value);
const shippingFee = computed(() => (amount.value >= 499000 ? 0 : 30000));
const total = computed(() => amount.value + shippingFee.value);

const formatPrice = (price) => Number(price || 0).toLocaleString("vi-VN");

const removeItem = (item) => store.removeFromCart(item.productId, item.variantId ?? null);

const changeQty = (item, qty) => {
  if (qty < 1) {
    removeItem(item);
    return;
  }
  store.updateCartQuantity(item.productId, qty, item.variantId ?? null);
};
</script>
