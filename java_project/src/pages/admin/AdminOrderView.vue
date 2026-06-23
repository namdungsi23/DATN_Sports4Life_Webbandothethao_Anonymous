<template>
    <AdminLayout>
      <div v-if="err" class="alert alert-danger">{{ err }}</div>
  
      <div class="card mb-4">
        <div class="card-header fw-bold">Danh sách đơn hàng</div>
        <div class="card-body p-0">
          <table class="table table-hover mb-0">
            <thead class="table-dark">
              <tr>
                <th>ID</th>
                <th>User</th>
                <th>Ngày tạo</th>
                <th>Địa chỉ</th>
                <th>Trạng thái</th>
                <th>Tổng tiền</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="o in orders" :key="o.id">
                <td>{{ o.id }}</td>
                <td>{{ o.username }}</td>
                <td>{{ o.createDate }}</td>
                <td>{{ o.address }}</td>
                <td>
                  <span v-if="o.status === 'NEW'" class="badge bg-warning">NEW</span>
                  <span v-else-if="o.status === 'CONFIRMED'" class="badge bg-success">CONFIRMED</span>
                  <span v-else-if="o.status === 'CANCELLED'" class="badge bg-danger">CANCELLED</span>
                  <span v-else class="badge bg-secondary">{{ o.status }}</span>
                </td>
                <td>{{ formatPrice(o.totalAmount) }} ₫</td>
                <td>
                  <button type="button" class="btn btn-sm btn-info" @click="openDetail(o.id)">Xem chi tiết</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
  
      <div v-if="selectedOrderId != null" class="card mb-3">
        <div class="card-body">
          <form class="d-flex align-items-center gap-3 flex-wrap" @submit.prevent="updateStatus">
            <label class="fw-bold mb-0">Trạng thái:</label>
            <select v-model="statusPick" class="form-select w-auto">
              <option value="NEW">NEW</option>
              <option value="CONFIRMED">CONFIRMED</option>
              <option value="CANCELLED">CANCELLED</option>
            </select>
            <button type="submit" class="btn btn-success">💾 Cập nhật</button>
          </form>
        </div>
      </div>
  
      <div v-if="orderDetails.length" class="card">
        <div class="card-header fw-bold">
            Chi tiết đơn hàng #
          {{ selectedOrderId }}
        </div>
        <div class="card-body p-0">
          <table class="table table-bordered mb-0">
            <thead class="table-secondary">
              <tr>
                <th>ID</th>
                <th>Sản phẩm</th>
                <th>Giá</th>
                <th>Số lượng</th>
                <th>Thành tiền</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="d in orderDetails" :key="d.id">
                <td>{{ d.id }}</td>
                <td>{{ d.productName }}</td>
                <td>{{ formatPrice(d.price) }}</td>
                <td>{{ d.quantity }}</td>
                <td>{{ formatPrice(d.lineTotal) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </AdminLayout>
  </template>
  
  <script setup>
  import { onMounted, ref, watch } from "vue";
  import { useRoute, useRouter } from "vue-router";
  import AdminLayout from "../../layouts/AdminLayout.vue";
  import { apiFetch } from "../../services/http.js";

  const route = useRoute();
  const router = useRouter();
  const err = ref("");
  const orders = ref([]);
  const selectedOrderId = ref(null);
  const statusPick = ref("NEW");
  const orderDetails = ref([]);
  
  function formatPrice(n) {
    return Number(n || 0).toLocaleString("vi-VN");
  }
  
  async function loadList() {
    err.value = "";
    const data = await apiFetch("/api/admin/orders");
    orders.value = data.orders || [];
  }
  
  async function loadDetail(id) {
    err.value = "";
    const data = await apiFetch(`/api/admin/orders/${id}`);
    orders.value = data.orders || [];
    selectedOrderId.value = data.selectedOrderId;
    orderDetails.value = data.orderDetails || [];
    const cur = orders.value.find((o) => o.id === id);
    if (cur) statusPick.value = cur.status || "NEW";
  }
  
  function openDetail(id) {
    router.push({ path: `/admin/order/${id}` });
  }
  
  async function updateStatus() {
    if (selectedOrderId.value == null) return;
    try {
      await apiFetch("/api/admin/orders/update-status", {
        method: "POST",
        body: JSON.stringify({ orderId: selectedOrderId.value, status: statusPick.value }),
      });
      await loadDetail(selectedOrderId.value);
    } catch (e) {
      err.value = e.message || "Cập nhật thất bại";
    }
  }
  
  onMounted(async () => {
    try {
      const id = route.params.id ? parseInt(route.params.id, 10) : NaN;
      if (!Number.isNaN(id)) {
        await loadDetail(id);
      } else {
        await loadList();
      }
    } catch (e) {
      err.value = e.message || "Không tải được đơn hàng";
    }
  });
  
  watch(
    () => route.params.id,
    async (pid) => {
      const id = pid ? parseInt(pid, 10) : NaN;
      try {
        if (!Number.isNaN(id)) await loadDetail(id);
        else {
          selectedOrderId.value = null;
          orderDetails.value = [];
          await loadList();
        }
      } catch (e) {
        err.value = e.message || "Lỗi";
      }
    }
  );
  </script>
  