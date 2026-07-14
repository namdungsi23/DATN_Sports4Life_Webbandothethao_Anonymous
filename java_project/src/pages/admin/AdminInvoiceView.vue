<template>
  <AdminLayout>
    <div class="admin-invoice">
      <div class="admin-invoice__head">
        <div>
          <h1>Quản lý hóa đơn</h1>
          <p>Tổng hợp doanh thu theo ngày, tháng, năm — <strong>mỗi đơn hàng = 1 hóa đơn</strong></p>
          <p v-if="filterLabel" class="admin-invoice__range small text-muted mb-0">{{ filterLabel }}</p>
        </div>
        <div class="admin-invoice__filters no-print">
          <input v-model="filters.from" type="date" class="form-control" />
          <span class="text-muted">đến</span>
          <input v-model="filters.to" type="date" class="form-control" />
          <div class="btn-group">
            <button
              v-for="p in periods"
              :key="p.value"
              type="button"
              class="btn btn-sm"
              :class="period === p.value ? 'btn-primary' : 'btn-outline-secondary'"
              @click="period = p.value; loadCharts()"
            >
              {{ p.label }}
            </button>
          </div>
          <button type="button" class="btn btn-success btn-sm" @click="loadAll">Áp dụng</button>
          <button type="button" class="btn btn-outline-secondary btn-sm" @click="resetAllTime">Tất cả thời gian</button>
        </div>
      </div>

      <div v-if="invoiceMeta.excludedCancelled || invoiceMeta.excludedOutOfRange" class="alert alert-info py-2 small no-print">
        <template v-if="invoiceMeta.excludedCancelled">
          Có <strong>{{ invoiceMeta.excludedCancelled }}</strong> đơn đã hủy không hiển thị trong hóa đơn.
        </template>
        <template v-if="invoiceMeta.excludedOutOfRange">
          <span v-if="invoiceMeta.excludedCancelled"> · </span>
          Có <strong>{{ invoiceMeta.excludedOutOfRange }}</strong> đơn nằm ngoài khoảng ngày đang lọc.
        </template>
        <span class="text-muted"> (Tổng {{ invoiceMeta.totalOrdersAll }} đơn trong hệ thống)</span>
      </div>

      <div v-if="err" class="alert alert-danger">{{ err }}</div>

      <div class="admin-stat-grid">
        <div v-for="card in kpiCards" :key="card.label" class="admin-stat-card admin-stat-card--premium">
          <div class="admin-stat-card__icon" :class="`admin-stat-card__icon--${card.tone}`">
            <AdminIcon :name="card.icon" :size="22" />
          </div>
          <div>
            <p class="admin-stat-card__value">{{ card.value }}</p>
            <p class="admin-stat-card__label">{{ card.label }}</p>
          </div>
        </div>
      </div>

      <div class="admin-chart-grid">
        <div class="admin-panel admin-panel--chart">
          <div class="admin-panel__head">
            <h2><AdminIcon name="chart" :size="18" /> Doanh thu theo {{ periodLabel }}</h2>
          </div>
          <div class="admin-panel__body">
            <canvas ref="revenueChartRef" height="120"></canvas>
          </div>
        </div>

        <div class="admin-panel admin-panel--chart">
          <div class="admin-panel__head">
            <h2><AdminIcon name="order" :size="18" /> Số hóa đơn theo {{ periodLabel }}</h2>
          </div>
          <div class="admin-panel__body">
            <canvas ref="countChartRef" height="120"></canvas>
          </div>
        </div>

        <div class="admin-panel admin-panel--chart admin-panel--half">
          <div class="admin-panel__head">
            <h2>Trạng thái đơn</h2>
          </div>
          <div class="admin-panel__body admin-panel__body--donut">
            <canvas ref="statusChartRef" height="160"></canvas>
          </div>
        </div>

        <div class="admin-panel admin-panel--chart admin-panel--half">
          <div class="admin-panel__head">
            <h2>Thanh toán</h2>
          </div>
          <div class="admin-panel__body admin-panel__body--donut">
            <canvas ref="paymentChartRef" height="160"></canvas>
          </div>
        </div>
      </div>

      <div class="admin-panel mb-4">
        <div class="admin-panel__head d-flex justify-content-between align-items-center">
          <h2><AdminIcon name="users" :size="18" /> Hóa đơn theo khách hàng</h2>
          <span class="text-muted small">{{ userSummary.totalUsers }} khách · {{ userSummary.totalInvoices }} hóa đơn · {{ formatMoney(userSummary.totalRevenue) }}</span>
        </div>
        <div class="admin-panel__body p-0">
          <table class="table table-hover mb-0 align-middle">
            <thead class="table-light">
              <tr>
                <th>Khách hàng</th>
                <th class="text-center">Số HĐ</th>
                <th class="text-end">Tổng tiền</th>
                <th>Chi tiết từng hóa đơn</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in userRows" :key="user.accountId || user.username">
                <td>
                  <strong>{{ user.username || "—" }}</strong>
                  <div v-if="user.email" class="small text-muted">{{ user.email }}</div>
                </td>
                <td class="text-center">
                  <span class="badge bg-primary">{{ user.invoiceCount }}</span>
                </td>
                <td class="text-end fw-bold text-success">{{ formatMoney(user.totalSpent) }}</td>
                <td>
                  <div class="user-invoice-list">
                    <div v-for="inv in user.invoices" :key="inv.id" class="user-invoice-list__item">
                      <RouterLink :to="`/admin/order/${inv.id}`" class="fw-semibold">{{ inv.invoiceCode }}</RouterLink>
                      <span class="text-muted"> · {{ formatDate(inv.createDate) }}</span>
                      <span class="text-success fw-semibold"> · {{ formatMoney(inv.totalAmount) }}</span>
                      <span class="badge ms-1" :class="orderStatusBadgeClass(inv.orderStatus)">{{ statusLabel(inv.orderStatus) }}</span>
                    </div>
                  </div>
                </td>
              </tr>
              <tr v-if="!userRows.length && !loading">
                <td colspan="4" class="text-center text-muted py-4">Không có dữ liệu khách hàng trong khoảng thời gian này.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="admin-panel">
        <div class="admin-panel__head d-flex justify-content-between align-items-center">
          <h2><AdminIcon name="order" :size="18" /> Danh sách hóa đơn</h2>
          <span class="text-muted small">{{ invoiceMeta.totalElements }} hóa đơn · {{ formatMoney(invoiceMeta.totalRevenue) }}</span>
        </div>
        <div class="admin-panel__body p-0">
          <table class="table table-hover mb-0 align-middle">
            <thead class="table-light">
              <tr>
                <th>Mã HĐ</th>
                <th>Khách hàng</th>
                <th>Ngày</th>
                <th>Trạng thái</th>
                <th>Thanh toán</th>
                <th class="text-end">Tổng tiền</th>
                <th class="text-end no-print">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="inv in invoices" :key="inv.id">
                <td><strong>{{ inv.invoiceCode }}</strong></td>
                <td>{{ inv.username || "—" }}</td>
                <td>{{ formatDate(inv.createDate) }}</td>
                <td>
                  <span class="badge" :class="orderStatusBadgeClass(inv.orderStatus)">
                    {{ statusLabel(inv.orderStatus) }}
                  </span>
                </td>
                <td>
                  <span class="badge" :class="inv.paymentStatus === 'PAID' ? 'bg-success' : 'bg-secondary'">
                    {{ paymentLabel(inv.paymentStatus) }}
                  </span>
                </td>
                <td class="text-end fw-bold text-success">{{ formatMoney(inv.totalAmount) }}</td>
                <td class="text-end text-nowrap no-print">
                  <RouterLink :to="`/admin/order/${inv.id}`" class="btn btn-sm btn-outline-primary me-1">Chi tiết</RouterLink>
                  <button type="button" class="btn btn-sm btn-outline-secondary" @click="openPrint(inv.id)">In HĐ</button>
                </td>
              </tr>
              <tr v-if="!invoices.length && !loading">
                <td colspan="7" class="text-center text-muted py-4">Không có hóa đơn trong khoảng thời gian này.</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="invoiceMeta.totalPages > 1" class="admin-panel__footer d-flex justify-content-center gap-2 py-3 no-print">
          <button type="button" class="btn btn-sm btn-outline-secondary" :disabled="page <= 0" @click="changePage(page - 1)">← Trước</button>
          <span class="align-self-center small text-muted">Trang {{ page + 1 }} / {{ invoiceMeta.totalPages }}</span>
          <button type="button" class="btn btn-sm btn-outline-secondary" :disabled="page >= invoiceMeta.totalPages - 1" @click="changePage(page + 1)">Sau →</button>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, shallowRef } from "vue";
import { RouterLink } from "vue-router";
import { Chart, registerables } from "chart.js";
import AdminLayout from "../../layouts/AdminLayout.vue";
import AdminIcon from "../../components/admin/AdminIcon.vue";
import { apiFetch } from "../../services/http.js";
import {
  ORDER_STATUS_LABELS,
  PAYMENT_STATUS_LABELS,
  orderStatusBadgeClass,
} from "../../utils/adminAccess";

Chart.register(...registerables);

const CHART_COLORS = {
  green: "#1a3c34",
  orange: "#e8871e",
  blue: "#3b82f6",
  purple: "#8b5cf6",
  teal: "#14b8a6",
  red: "#ef4444",
  gray: "#9ca3af",
};

const err = ref("");
const loading = ref(false);
const period = ref("month");
const page = ref(0);
const filters = ref({ from: defaultFrom(), to: defaultTo() });
const summary = ref({});
const invoices = ref([]);
const userRows = ref([]);
const userSummary = ref({ totalUsers: 0, totalInvoices: 0, totalRevenue: 0 });
const invoiceMeta = ref({
  totalElements: 0,
  totalPages: 1,
  totalRevenue: 0,
  totalOrdersAll: 0,
  excludedCancelled: 0,
  excludedOutOfRange: 0,
});

const revenueChartRef = ref(null);
const countChartRef = ref(null);
const statusChartRef = ref(null);
const paymentChartRef = ref(null);
const charts = shallowRef([]);

const periods = [
  { value: "day", label: "Ngày" },
  { value: "month", label: "Tháng" },
  { value: "year", label: "Năm" },
];

const periodLabel = computed(() => periods.find((p) => p.value === period.value)?.label || "Tháng");

const kpiCards = computed(() => [
  { icon: "chart", label: "Doanh thu", value: formatMoney(summary.value.totalRevenue), tone: "green" },
  { icon: "order", label: "Số hóa đơn", value: summary.value.invoiceCount ?? summary.value.orderCount ?? 0, tone: "orange" },
  { icon: "package", label: "Giá trị TB/HĐ", value: formatMoney(summary.value.averageOrderValue), tone: "blue" },
  { icon: "users", label: "Đã thanh toán", value: summary.value.paidCount ?? 0, tone: "teal" },
  { icon: "settings", label: "Chưa thanh toán", value: summary.value.unpaidCount ?? 0, tone: "purple" },
]);

const filterLabel = computed(() => {
  if (!filters.value.from && !filters.value.to) return "";
  return `Đang lọc: ${formatFilterDate(filters.value.from)} → ${formatFilterDate(filters.value.to)} (giờ Việt Nam)`;
});

function localDateStr(date = new Date()) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

function defaultFrom() {
  const d = new Date();
  d.setMonth(d.getMonth() - 11);
  d.setDate(1);
  return localDateStr(d);
}

function defaultTo() {
  return localDateStr();
}

function formatFilterDate(value) {
  if (!value) return "—";
  const [y, m, d] = value.split("-");
  return `${d}/${m}/${y}`;
}

function resetAllTime() {
  filters.value = { from: "2000-01-01", to: defaultTo() };
  page.value = 0;
  loadAll();
}

function queryParams() {
  return `from=${filters.value.from}&to=${filters.value.to}`;
}

const formatMoney = (v) => `${Number(v || 0).toLocaleString("vi-VN")} ₫`;
const formatDate = (v) => {
  if (!v) return "—";
  return new Date(v).toLocaleString("vi-VN", { timeZone: "Asia/Ho_Chi_Minh" });
};
const statusLabel = (s) => ORDER_STATUS_LABELS[s] || s;
const paymentLabel = (s) => PAYMENT_STATUS_LABELS[s] || s;

function destroyCharts() {
  charts.value.forEach((c) => c.destroy());
  charts.value = [];
}

function makeChart(canvas, config) {
  if (!canvas) return;
  const chart = new Chart(canvas, config);
  charts.value.push(chart);
}

async function loadSummary() {
  summary.value = await apiFetch(`/api/admin/reports/summary?${queryParams()}`);
}

async function loadInvoices() {
  const data = await apiFetch(`/api/admin/reports/invoices?${queryParams()}&page=${page.value}&size=500`);
  invoices.value = data.items || [];
  invoiceMeta.value = {
    totalElements: data.totalElements ?? 0,
    totalPages: data.totalPages ?? 1,
    totalRevenue: data.totalRevenue ?? 0,
    totalOrdersAll: data.totalOrdersAll ?? 0,
    excludedCancelled: data.excludedCancelled ?? 0,
    excludedOutOfRange: data.excludedOutOfRange ?? 0,
  };
}

async function loadUserSummary() {
  const data = await apiFetch(`/api/admin/reports/by-user?${queryParams()}`);
  userRows.value = data.users || [];
  userSummary.value = {
    totalUsers: data.totalUsers ?? 0,
    totalInvoices: data.totalInvoices ?? 0,
    totalRevenue: data.totalRevenue ?? 0,
  };
}

async function loadCharts() {
  const [revenue, breakdown] = await Promise.all([
    apiFetch(`/api/admin/reports/revenue?period=${period.value}&${queryParams()}`),
    apiFetch(`/api/admin/reports/status-breakdown?${queryParams()}`),
  ]);

  destroyCharts();

  const labels = revenue.labels || [];
  const revData = (revenue.revenue || []).map(Number);
  const countData = revenue.orderCounts || [];

  makeChart(revenueChartRef.value, {
    type: period.value === "year" ? "bar" : "line",
    data: {
      labels,
      datasets: [{
        label: "Doanh thu (₫)",
        data: revData,
        borderColor: CHART_COLORS.green,
        backgroundColor: period.value === "year" ? CHART_COLORS.orange : "rgba(26,60,52,0.15)",
        fill: period.value !== "year",
        tension: 0.35,
        borderWidth: 2,
      }],
    },
    options: chartOptions("₫"),
  });

  makeChart(countChartRef.value, {
    type: "bar",
    data: {
      labels,
      datasets: [{
        label: "Số đơn",
        data: countData,
        backgroundColor: CHART_COLORS.orange,
        borderRadius: 6,
      }],
    },
    options: chartOptions("đơn"),
  });

  const statusData = breakdown.orderStatus || {};
  makeChart(statusChartRef.value, {
    type: "doughnut",
    data: {
      labels: Object.keys(statusData).map(statusLabel),
      datasets: [{
        data: Object.values(statusData),
        backgroundColor: [CHART_COLORS.orange, CHART_COLORS.blue, CHART_COLORS.green, CHART_COLORS.teal, CHART_COLORS.red],
      }],
    },
    options: donutOptions(),
  });

  const payData = breakdown.paymentStatus || {};
  makeChart(paymentChartRef.value, {
    type: "doughnut",
    data: {
      labels: Object.keys(payData).map(paymentLabel),
      datasets: [{
        data: Object.values(payData),
        backgroundColor: [CHART_COLORS.green, CHART_COLORS.gray],
      }],
    },
    options: donutOptions(),
  });
}

function chartOptions(unit) {
  return {
    responsive: true,
    maintainAspectRatio: true,
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          label: (ctx) => unit === "₫"
            ? `${Number(ctx.raw).toLocaleString("vi-VN")} ₫`
            : `${ctx.raw} đơn`,
        },
      },
    },
    scales: {
      y: {
        beginAtZero: true,
        ticks: {
          callback: (v) => unit === "₫" ? `${(v / 1000).toFixed(0)}k` : v,
        },
      },
    },
  };
}

function donutOptions() {
  return {
    responsive: true,
    maintainAspectRatio: true,
    plugins: { legend: { position: "bottom" } },
  };
}

async function loadAll() {
  loading.value = true;
  err.value = "";
  try {
    await Promise.all([loadSummary(), loadInvoices(), loadUserSummary(), loadCharts()]);
  } catch (e) {
    err.value = e?.message || "Không tải được báo cáo hóa đơn.";
  } finally {
    loading.value = false;
  }
}

function changePage(p) {
  page.value = p;
  loadInvoices().catch((e) => { err.value = e?.message; });
}

function openPrint(orderId) {
  window.open(`/admin/order/${orderId}?print=1`, "_blank");
}

onMounted(loadAll);
onBeforeUnmount(destroyCharts);
</script>

<style scoped>
.admin-invoice__head {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.admin-invoice__head h1 {
  font-family: "Playfair Display", Georgia, serif;
  font-size: 28px;
  color: #1a3c34;
  margin: 0 0 4px;
}

.admin-invoice__head p {
  color: #6b7280;
  margin: 0;
  font-size: 14px;
}

.admin-invoice__filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.admin-invoice__filters .form-control {
  width: 150px;
}

.admin-stat-card--premium {
  background: linear-gradient(135deg, #fff 0%, #f8faf9 100%);
}

.admin-chart-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.admin-panel--chart .admin-panel__body {
  min-height: 280px;
}

.admin-panel--half {
  grid-column: span 1;
}

.admin-panel__body--donut {
  display: flex;
  align-items: center;
  justify-content: center;
  max-width: 320px;
  margin: 0 auto;
}

.admin-panel__footer {
  border-top: 1px solid #f0f2f5;
}

.user-invoice-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.user-invoice-list__item {
  font-size: 13px;
  line-height: 1.4;
}

.admin-invoice__range {
  margin-top: 4px;
}

@media (max-width: 992px) {
  .admin-chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>
