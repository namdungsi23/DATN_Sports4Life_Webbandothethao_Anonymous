<template>
  <MainLayout>
    <div class="container my-4">
      <h2 class="mb-2">Thanh toán</h2>
      <div v-if="error" class="alert alert-danger py-2">{{ error }}</div>

      <div class="card mb-4">
        <div class="card-header fw-bold">Thông tin giỏ hàng</div>
        <div class="card-body p-0">
          <table class="table table-bordered mb-0 text-center align-middle">
            <thead class="table-light">
              <tr><th>Sản phẩm</th><th>Giá</th><th>Số lượng</th><th>Tạm tính</th></tr>
            </thead>
            <tbody>
              <tr v-for="item in cartItems" :key="item.productId">
                <td>{{ item.name }}</td>
                <td>{{ formatPrice(item.price) }} đ</td>
                <td>{{ item.quantity }}</td>
                <td class="fw-bold text-danger">{{ formatPrice(item.price * item.quantity) }} đ</td>
              </tr>
            </tbody>
          </table>
          <div class="card-footer text-end fw-bold fs-5 my-4">
            Tổng tiền: <span class="text-danger">{{ formatPrice(amount) }}</span>
          </div>
        </div>
      </div>

      <form @submit.prevent="submitPayment">
        <div class="card mb-4">
          <div class="card-header fw-bold">Phương thức thanh toán</div>
          <div class="card-body">
            <div class="form-check mb-2" v-for="method in methods" :key="method.value">
              <input :id="method.value" v-model="paymentMethod" class="form-check-input" type="radio" :value="method.value" />
              <label :for="method.value" class="form-check-label">{{ method.label }}</label>
            </div>
          </div>
        </div>
        <button class="btn btn-primary mt-3" :disabled="loading || !cartItems.length">
          {{ loading ? "Đang xử lý..." : "Tiếp tục thanh toán" }}
        </button>
      </form>
    </div>
  </MainLayout>
</template>

<script setup>
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import MainLayout from "../../layouts/MainLayout.vue";
import { submitCheckoutApi } from "../../services/api";
import { useAppStore } from "../../stores/appStore";

const router = useRouter();
const store = useAppStore();
const cartItems = computed(() => store.state.cartItems);
const paymentMethod = ref("CASH");
const loading = ref(false);
const error = ref("");

const methods = [
  { value: "CASH", label: "Thanh toán khi nhận hàng" },
  { value: "MOMO", label: "Ví điện tử MOMO" },
  { value: "TECHCOMBANK", label: "Chuyển khoản Techcombank" },
];

const amount = computed(() => store.cartAmount.value);
const formatPrice = (price) => Number(price || 0).toLocaleString("vi-VN");

const submitPayment = async () => {
  if (!cartItems.value.length) {
    error.value = "Giỏ hàng đang trống.";
    return;
  }

  loading.value = true;
  error.value = "";
  const payload = {
    paymentMethod: paymentMethod.value,
    amount: amount.value,
    items: cartItems.value,
  };

  try {
    await submitCheckoutApi(payload);
  } catch (submitError) {
    console.warn("Checkout API not ready, fallback to client flow.", submitError);
  } finally {
    loading.value = false;
  }

  router.push({
    path: "/cart/payment",
    query: { method: paymentMethod.value, amount: String(amount.value) },
  });
};
</script>
  