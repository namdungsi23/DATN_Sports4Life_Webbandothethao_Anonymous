<template>
  <AdminLayout>
    <!-- Hóa đơn in -->
    <div v-if="selectedOrderId && orderDetail" class="invoice-print-area s4l-card mb-4" :class="{ 'invoice-print-area--solo': isPrintMode }">
      <div class="invoice-print-area__header">
        <div>
          <h2 class="invoice-print-area__brand">Sports4Life</h2>
          <p class="invoice-print-area__sub">Hóa đơn bán hàng</p>
        </div>
        <div class="text-end">
          <p class="mb-1"><strong>Mã HĐ:</strong> HD-{{ String(selectedOrderId).padStart(6, "0") }}</p>
          <p class="mb-0"><strong>Ngày:</strong> {{ formatDate(orderDetail.createDate) }}</p>
        </div>
      </div>

      <div class="invoice-print-area__parties">
        <div>
          <h6>Người bán</h6>
          <p class="mb-0">Sports4Life — Cửa hàng thể thao</p>
          <p class="mb-0 text-muted small">Hotline: 1900 xxxx</p>
        </div>
        <div>
          <h6>Người mua</h6>
          <p class="mb-1"><strong>{{ orderDetail.shippingAddress?.receiverName || orderDetail.customer?.username || "—" }}</strong></p>
          <p class="mb-1 small">{{ orderDetail.customer?.email || "" }}</p>
          <p class="mb-1 small">{{ orderDetail.shippingAddress?.receiverPhone || "" }}</p>
          <p class="mb-0 small" v-if="orderDetail.shippingAddress">
            {{ orderDetail.shippingAddress.addressDetail }}, {{ orderDetail.shippingAddress.ward }}, {{ orderDetail.shippingAddress.province }}
          </p>
        </div>
      </div>

      <table class="table table-bordered invoice-print-area__table">
        <thead>
          <tr>
            <th>#</th>
            <th>Sản phẩm</th>
            <th>Biến thể</th>
            <th class="text-end">Đơn giá</th>
            <th class="text-center">SL</th>
            <th class="text-end">Thành tiền</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(d, i) in orderItems" :key="d.id">
            <td>{{ i + 1 }}</td>
            <td>{{ d.productName || "—" }}</td>
            <td>{{ variantLabel(d) }}</td>
            <td class="text-end">{{ formatPrice(d.price) }} ₫</td>
            <td class="text-center">{{ d.quantity }}</td>
            <td class="text-end">{{ formatPrice(d.lineTotal) }} ₫</td>
          </tr>
        </tbody>
      </table>

      <div class="invoice-print-area__totals">
        <div class="invoice-print-area__totals-row">
          <span>Tạm tính</span>
          <span>{{ formatPrice(orderDetail.subTotal) }} ₫</span>
        </div>
        <div class="invoice-print-area__totals-row" v-if="form.shippingFee">
          <span>Phí vận chuyển</span>
          <span>{{ formatPrice(form.shippingFee) }} ₫</span>
        </div>
        <div class="invoice-print-area__totals-row invoice-print-area__totals-row--grand">
          <span>Tổng cộng</span>
          <span>{{ formatPrice(orderDetail.totalAmount) }} ₫</span>
        </div>
        <div class="invoice-print-area__status">
          <span class="badge" :class="orderStatusBadgeClass(orderDetail.orderStatus)">{{ statusLabel(orderDetail.orderStatus) }}</span>
          <span class="badge ms-2" :class="orderDetail.paymentStatus === 'PAID' ? 'bg-success' : 'bg-secondary'">
            {{ paymentLabel(orderDetail.paymentStatus) }}
          </span>
        </div>
      </div>

      <p class="invoice-print-area__thanks text-center text-muted mt-4 mb-0">Cảm ơn quý khách đã mua hàng tại Sports4Life!</p>

      <div v-if="!isPrintMode" class="invoice-print-area__actions no-print text-end mt-3">
        <button type="button" class="btn btn-outline-secondary btn-sm" @click="printInvoice">In hóa đơn</button>
      </div>
    </div>

    <div class="no-print" :class="{ 'd-none': isPrintMode }">
    <div v-if="flashOk" class="alert alert-success">{{ flashOk }}</div>
    <div v-if="err" class="alert alert-danger">{{ err }}</div>

    <div v-if="!selectedOrderId" class="card mb-4 border-warning">
      <div class="card-body d-flex flex-wrap align-items-center justify-content-between gap-3">
        <div>
          <h5 class="mb-1">Đơn chờ xác nhận</h5>
          <p class="mb-0 text-muted">
            Có <strong class="text-danger">{{ pendingCount }}</strong> đơn hàng đang chờ nhân viên xác nhận.
          </p>
        </div>
        <button
          type="button"
          class="btn btn-warning"
          :class="{ active: filterStatus === 'PENDING' }"
          @click="setFilter('PENDING')"
        >
          Xem đơn chờ xác nhận
        </button>
      </div>
    </div>

    <div class="card mb-4">
      <div class="card-header d-flex flex-wrap align-items-center justify-content-between gap-2">
        <span class="fw-bold">{{ selectedOrderId ? `Đơn hàng #${selectedOrderId}` : "Danh sách đơn hàng" }}</span>
        <div class="btn-group btn-group-sm">
          <button type="button" class="btn btn-outline-secondary" :class="{ active: filterStatus === 'ALL' }" @click="setFilter('ALL')">
            Tất cả
          </button>
          <button type="button" class="btn btn-outline-warning" :class="{ active: filterStatus === 'PENDING' }" @click="setFilter('PENDING')">
            Chờ xác nhận
          </button>
        </div>
      </div>
      <div class="card-body p-0">
        <table class="table table-hover mb-0 align-middle">
          <thead class="table-dark">
            <tr>
              <th>ID</th>
              <th>Khách hàng</th>
              <th>Ngày tạo</th>
              <th>Địa chỉ</th>
              <th>Trạng thái</th>
              <th>Thanh toán</th>
              <th>Tổng tiền</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="o in filteredOrders" :key="o.id">
              <td>{{ o.id }}</td>
              <td>{{ o.username }}</td>
              <td>{{ formatDate(o.createDate) }}</td>
              <td class="small">{{ o.address || "—" }}</td>
              <td>
                <span class="badge" :class="orderStatusBadgeClass(o.orderStatus)">
                  {{ statusLabel(o.orderStatus) }}
                </span>
              </td>
              <td>
                <span class="badge" :class="o.paymentStatus === 'PAID' ? 'bg-success' : 'bg-secondary'">
                  {{ paymentLabel(o.paymentStatus) }}
                </span>
              </td>
              <td>{{ formatPrice(o.totalAmount) }} ₫</td>
              <td class="text-nowrap">
                <button type="button" class="btn btn-sm btn-outline-primary me-1" @click="openDetail(o.id)">
                  Chi tiết
                </button>
                <button
                  v-if="canUpdate && o.orderStatus === 'PENDING'"
                  type="button"
                  class="btn btn-sm btn-success"
                  :disabled="confirmingId === o.id"
                  @click="quickConfirm(o.id)"
                >
                  Xác nhận
                </button>
              </td>
            </tr>
            <tr v-if="!filteredOrders.length">
              <td colspan="8" class="text-center text-muted py-4">Không có đơn hàng.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="selectedOrderId && orderDetail" class="card mb-4">
      <div class="card-header fw-bold">Cập nhật đơn hàng #{{ selectedOrderId }}</div>
      <div class="card-body">
        <div class="row g-3 mb-4">
          <div class="col-md-6">
            <h6 class="text-muted mb-2">Thông tin khách hàng</h6>
            <p class="mb-1"><strong>Tài khoản:</strong> {{ orderDetail.customer?.username || "—" }}</p>
            <p class="mb-1"><strong>Email:</strong> {{ orderDetail.customer?.email || "—" }}</p>
            <template v-if="orderDetail.shippingAddress">
              <p class="mb-1"><strong>Người nhận:</strong> {{ orderDetail.shippingAddress.receiverName }}</p>
              <p class="mb-1"><strong>SĐT:</strong> {{ orderDetail.shippingAddress.receiverPhone }}</p>
              <p class="mb-0">
                <strong>Địa chỉ:</strong>
                {{ orderDetail.shippingAddress.addressDetail }}, {{ orderDetail.shippingAddress.ward }},
                {{ orderDetail.shippingAddress.province }}
              </p>
            </template>
          </div>
          <div class="col-md-6">
            <h6 class="text-muted mb-2">Thông tin đơn</h6>
            <p class="mb-1"><strong>Tạo lúc:</strong> {{ formatDate(orderDetail.createDate) }}</p>
            <p class="mb-1"><strong>Cập nhật:</strong> {{ formatDate(orderDetail.updateDate) || "—" }}</p>
            <p class="mb-1"><strong>Tạm tính:</strong> {{ formatPrice(orderDetail.subTotal) }} ₫</p>
            <p class="mb-0"><strong>Tổng:</strong> {{ formatPrice(orderDetail.totalAmount) }} ₫</p>
          </div>
        </div>

        <form v-if="canUpdate" class="row g-3" @submit.prevent="saveOrder">
          <div class="col-md-4">
            <label class="form-label fw-semibold">Trạng thái đơn</label>
            <select v-model="form.orderStatus" class="form-select">
              <option v-for="s in allowedOrderStatuses" :key="s" :value="s">{{ statusLabel(s) }}</option>
            </select>
          </div>
          <div class="col-md-4">
            <label class="form-label fw-semibold">Thanh toán</label>
            <select v-model="form.paymentStatus" class="form-select">
              <option v-for="s in allowedPaymentStatuses" :key="s" :value="s">{{ paymentLabel(s) }}</option>
            </select>
          </div>
          <div class="col-md-4 d-flex align-items-end gap-2">
            <button type="submit" class="btn btn-primary" :disabled="saving">Lưu thay đổi</button>
            <button
              v-if="form.orderStatus === 'PENDING'"
              type="button"
              class="btn btn-success"
              :disabled="saving"
              @click="confirmCurrent"
            >
              Xác nhận đơn
            </button>
            <button type="button" class="btn btn-outline-secondary" @click="backToList">Quay lại</button>
            <button type="button" class="btn btn-outline-dark" @click="printInvoice">In hóa đơn</button>
          </div>
        </form>
        <div v-else class="alert alert-info mb-0">Bạn chỉ có quyền xem thông tin đơn hàng.</div>

        <div v-if="showShipmentSection" class="border rounded p-3 mt-4 bg-light">
          <h6 class="mb-3">Vận chuyển</h6>
          <div class="row g-3">
            <div class="col-md-4">
              <label class="form-label">Đơn vị vận chuyển</label>
              <select v-model="form.carrierId" class="form-select" :disabled="!canUpdate">
                <option :value="null">-- Chọn --</option>
                <option v-for="c in carriers" :key="c.id" :value="c.id">{{ c.name }}</option>
              </select>
            </div>
            <div class="col-md-4">
              <label class="form-label">Mã vận đơn</label>
              <input v-model="form.trackingNumber" class="form-control" :readonly="!canUpdate" />
            </div>
            <div class="col-md-4">
              <label class="form-label">Trạng thái giao hàng</label>
              <select v-model="form.shippingStatus" class="form-select" :disabled="!canUpdate">
                <option v-for="s in allowedShippingStatuses" :key="s" :value="s">{{ shippingLabel(s) }}</option>
              </select>
            </div>
            <div class="col-md-4">
              <label class="form-label">Phí ship</label>
              <input v-model.number="form.shippingFee" type="number" min="0" step="1000" class="form-control" :readonly="!canUpdate" />
            </div>
            <div class="col-md-8">
              <label class="form-label">Ghi chú</label>
              <input v-model="form.notes" class="form-control" :readonly="!canUpdate" />
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="orderItems.length" class="card">
      <div class="card-header fw-bold">Sản phẩm trong đơn #{{ selectedOrderId }}</div>
      <div class="card-body p-0">
        <table class="table table-bordered mb-0">
          <thead class="table-secondary">
            <tr>
              <th>Sản phẩm</th>
              <th>SKU</th>
              <th>Biến thể</th>
              <th>Giá</th>
              <th>SL</th>
              <th>Thành tiền</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="d in orderItems" :key="d.id">
              <td>{{ d.productName || "—" }}</td>
              <td>{{ d.sku || "—" }}</td>
              <td>{{ variantLabel(d) }}</td>
              <td>{{ formatPrice(d.price) }} ₫</td>
              <td>{{ d.quantity }}</td>
              <td>{{ formatPrice(d.lineTotal) }} ₫</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import AdminLayout from "../../layouts/AdminLayout.vue";
import { apiFetch } from "../../services/http.js";
import { useAppStore } from "../../stores/appStore";
import { useAdminNotify } from "../../utils/adminNotify";
import {
  ORDER_STATUS_LABELS,
  PAYMENT_STATUS_LABELS,
  SHIPPING_STATUS_LABELS,
  orderStatusBadgeClass,
  userCanUpdateOrders,
} from "../../utils/adminAccess";

const route = useRoute();
const router = useRouter();
const store = useAppStore();
const notify = useAdminNotify();

const err = ref("");
const flashOk = ref("");
const orders = ref([]);
const pendingCount = ref(0);
const filterStatus = ref("ALL");
const selectedOrderId = ref(null);
const orderDetail = ref(null);
const orderItems = ref([]);
const allowedOrderStatuses = ref([]);
const allowedPaymentStatuses = ref([]);
const allowedShippingStatuses = ref([]);
const carriers = ref([]);
const saving = ref(false);
const confirmingId = ref(null);

const form = reactive({
  orderStatus: "PENDING",
  paymentStatus: "UNPAID",
  carrierId: null,
  trackingNumber: "",
  shippingStatus: "PENDING",
  shippingFee: 0,
  notes: "",
});

const canUpdate = computed(() => userCanUpdateOrders(store.state.user));
const isPrintMode = computed(() => route.query.print === "1");

function printInvoice() {
  window.print();
}

const filteredOrders = computed(() => {
  if (filterStatus.value === "PENDING") {
    return orders.value.filter((o) => o.orderStatus === "PENDING");
  }
  return orders.value;
});

const showShipmentSection = computed(() => {
  const status = form.orderStatus;
  return status === "SHIPPING" || status === "DELIVERED" || Boolean(orderDetail.value?.shipment);
});

function formatPrice(n) {
  return Number(n || 0).toLocaleString("vi-VN");
}

function formatDate(value) {
  if (!value) return "";
  return new Date(value).toLocaleString("vi-VN");
}

function statusLabel(status) {
  return ORDER_STATUS_LABELS[status] || status || "—";
}

function paymentLabel(status) {
  return PAYMENT_STATUS_LABELS[status] || status || "—";
}

function shippingLabel(status) {
  return SHIPPING_STATUS_LABELS[status] || status || "—";
}

function variantLabel(item) {
  const parts = [item.color, item.size].filter(Boolean);
  return parts.length ? parts.join(" / ") : "—";
}

function setFilter(status) {
  filterStatus.value = status;
}

async function loadList() {
  err.value = "";
  const data = await apiFetch("/api/admin/orders");
  orders.value = data.orders || [];
  pendingCount.value = data.pendingCount ?? orders.value.filter((o) => o.orderStatus === "PENDING").length;
}

function applyDetail(data) {
  orderDetail.value = data.order || null;
  orderItems.value = data.items || [];
  allowedOrderStatuses.value = data.allowedOrderStatuses || [];
  allowedPaymentStatuses.value = data.allowedPaymentStatuses || [];
  allowedShippingStatuses.value = data.allowedShippingStatuses || [];
  carriers.value = data.carriers || [];

  if (orderDetail.value) {
    form.orderStatus = orderDetail.value.orderStatus || "PENDING";
    form.paymentStatus = orderDetail.value.paymentStatus || "UNPAID";
  }

  const shipment = data.shipment;
  if (shipment) {
    form.carrierId = shipment.carrierId ?? null;
    form.trackingNumber = shipment.trackingNumber || "";
    form.shippingStatus = shipment.shippingStatus || "PENDING";
    form.shippingFee = Number(shipment.shippingFee || 0);
    form.notes = shipment.notes || "";
  } else {
    form.carrierId = null;
    form.trackingNumber = "";
    form.shippingStatus = "PENDING";
    form.shippingFee = 0;
    form.notes = "";
  }
}

async function loadDetail(id) {
  err.value = "";
  flashOk.value = "";
  const data = await apiFetch(`/api/admin/orders/${id}`);
  selectedOrderId.value = id;
  applyDetail(data);
  if (isPrintMode.value) {
    await nextTick();
    setTimeout(() => window.print(), 400);
  }
}

function openDetail(id) {
  router.push({ path: `/admin/order/${id}` });
}

function backToList() {
  router.push({ path: "/admin/order" });
}

async function quickConfirm(id) {
  confirmingId.value = id;
  err.value = "";
  flashOk.value = "";
  try {
    await apiFetch(`/api/admin/orders/${id}/confirm`, { method: "POST" });
    flashOk.value = `Đã xác nhận đơn #${id}`;
    notify.success(flashOk.value);
    await loadList();
    if (selectedOrderId.value === id) {
      await loadDetail(id);
    }
  } catch (e) {
    err.value = e.message || "Xác nhận thất bại";
    notify.error(err.value);
  } finally {
    confirmingId.value = null;
  }
}

async function confirmCurrent() {
  if (selectedOrderId.value == null) return;
  await quickConfirm(selectedOrderId.value);
}

async function saveOrder() {
  if (selectedOrderId.value == null || !canUpdate.value) return;

  if (showShipmentSection.value) {
    if (form.orderStatus === "SHIPPING" || form.shippingStatus === "SHIPPING") {
      if (!form.carrierId) {
        err.value = "Vui lòng chọn đơn vị vận chuyển khi giao hàng.";
        notify.error(err.value);
        return;
      }
      if (!String(form.trackingNumber || "").trim()) {
        err.value = "Vui lòng nhập mã vận đơn khi giao hàng.";
        notify.error(err.value);
        return;
      }
    }
    if (form.shippingFee != null && form.shippingFee !== "" && Number(form.shippingFee) < 0) {
      err.value = "Phí vận chuyển không được âm.";
      notify.error(err.value);
      return;
    }
  }

  saving.value = true;
  err.value = "";
  flashOk.value = "";
  try {
    const payload = {
      orderStatus: form.orderStatus,
      paymentStatus: form.paymentStatus,
    };
    if (showShipmentSection.value) {
      payload.carrierId = form.carrierId;
      payload.trackingNumber = form.trackingNumber;
      payload.shippingStatus = form.shippingStatus;
      payload.shippingFee = form.shippingFee;
      payload.notes = form.notes;
    }
    const data = await apiFetch(`/api/admin/orders/${selectedOrderId.value}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    });
    applyDetail(data);
    flashOk.value = "Cập nhật đơn hàng thành công";
    notify.success(flashOk.value);
    await loadList();
  } catch (e) {
    err.value = e.message || "Cập nhật thất bại";
    notify.error(err.value);
  } finally {
    saving.value = false;
  }
}

onMounted(async () => {
  try {
    const id = route.params.id ? parseInt(route.params.id, 10) : NaN;
    await loadList();
    if (!Number.isNaN(id)) {
      await loadDetail(id);
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
      if (!Number.isNaN(id)) {
        await loadDetail(id);
      } else {
        selectedOrderId.value = null;
        orderDetail.value = null;
        orderItems.value = [];
        await loadList();
      }
    } catch (e) {
      err.value = e.message || "Lỗi";
    }
  }
);
</script>
