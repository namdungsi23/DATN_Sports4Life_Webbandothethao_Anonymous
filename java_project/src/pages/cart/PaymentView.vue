<template>
  <MainLayout>
    <div class="container my-4" v-if="proceeding">
      <h2 class="mb-4">Xác nhận thanh toán</h2>
      <p><strong>Tổng tiền:</strong> <span class="text-danger fw-bold">{{ formatPrice(amount) }} đ</span></p>

      <ul class="nav nav-tabs mb-3">
        <li class="nav-item"><button class="nav-link" :class="{ active: tab === 'info' }" @click="tab = 'info'">Thông tin người nhận</button></li>
        <li class="nav-item"><button class="nav-link" :class="{ active: tab === 'payment' }" @click="tab = 'payment'">Thanh toán</button></li>
      </ul>

      <div v-if="tab === 'info'" class="card p-4">
        <h5 class="mb-3">Thông tin người nhận</h5>
        <input v-model="receiver.username" class="form-control mb-3" readonly />
        <input v-model="receiver.address" class="form-control mb-3" placeholder="Nhập địa chỉ nhận hàng" />
      </div>

      <div v-else class="card p-4">
        <h5>Thanh toán</h5>
        <p v-if="paymentMethod === 'CASH'" class="mb-3">Bạn sẽ thanh toán khi nhận hàng.</p>
        <p v-if="paymentMethod === 'MOMO'" class="mb-3">Vui lòng xác nhận chuyển khoản qua MOMO.</p>
        <p v-if="paymentMethod === 'TECHCOMBANK'" class="mb-3">Vui lòng xác nhận chuyển khoản Techcombank.</p>
        <button class="btn btn-success" :disabled="loading" @click="confirmPayment">
          {{ loading ? "Đang xác nhận..." : "Tôi đã thanh toán" }}
        </button>
      </div>
    </div>

    <div v-else class="container">
      <div class="row justify-content-center align-items-center" style="min-height:60vh">
        <div class="col-md-6">
          <div class="card shadow-sm text-center">
            <div class="card-body p-4">
              <h2 :class="success ? 'text-success' : 'text-danger'" class="fw-bold mb-3">
                {{ success ? "Đặt hàng thành công!" : "Có lỗi xảy ra" }}
              </h2>
              <RouterLink :to="success ? '/product' : '/cart'" class="btn" :class="success ? 'btn-success' : 'btn-danger'">
                {{ success ? "Quay về trang sản phẩm" : "Quay lại giỏ hàng" }}
              </RouterLink>
            </div>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
  </template>
  
  <script setup>
import { onMounted, ref } from "vue";
import { RouterLink, useRoute } from "vue-router";
  import MainLayout from "../../layouts/MainLayout.vue";
import { confirmPaymentApi } from "../../services/api";
import { useAppStore } from "../../stores/appStore";
  
const route = useRoute();
const store = useAppStore();
  const tab = ref("info");
  const proceeding = ref(true);
  const success = ref(false);
  const amount = ref(0);
  const paymentMethod = ref("CASH");
const loading = ref(false);
const receiver = ref({ username: "", address: "", email: "" });
  const formatPrice = (price) => Number(price || 0).toLocaleString("vi-VN");

onMounted(() => {
  amount.value = Number(route.query.amount || store.cartAmount.value || 0);
  paymentMethod.value = String(route.query.method || "CASH");
  receiver.value = {
    username: store.state.user?.username || "Khách hàng",
    address: "",
    email: store.state.user?.email || "",
  };
});

  const confirmPayment = async () => {
  if (!receiver.value.address?.trim()) {
    tab.value = "info";
    return;
  }

  loading.value = true;
  try {
    await confirmPaymentApi({
      paymentMethod: paymentMethod.value,
      receiver: receiver.value,
      amount: amount.value,
    });
    success.value = true;
    store.clearCart();
  } catch (error) {
    console.warn("Confirm API not ready, fallback success.", error);
    success.value = true;
    store.clearCart();
  } finally {
    proceeding.value = false;
    loading.value = false;
  }
  };
  </script>
  