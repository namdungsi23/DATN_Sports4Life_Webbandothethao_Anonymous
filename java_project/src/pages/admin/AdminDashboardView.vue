<template>
  <AdminLayout>
    <div class="admin-dashboard__toolbar">
      <div>
        <p>Tổng quan hoạt động cửa hàng Sports4Life — dữ liệu {{ monthsLabel }}.</p>
      </div>
      <div class="admin-dashboard__filters">
        <button
          v-for="opt in monthOptions"
          :key="opt.value"
          type="button"
          class="admin-dashboard__filter-btn"
          :class="{ 'is-active': months === opt.value }"
          @click="changeMonths(opt.value)"
        >
          {{ opt.label }}
        </button>
      </div>
    </div>

    <div v-if="err" class="alert alert-danger">{{ err }}</div>

    <div class="admin-stat-grid">
      <div v-for="card in statCards" :key="card.label" class="admin-stat-card">
        <div class="admin-stat-card__icon" :class="`admin-stat-card__icon--${card.tone}`">
          <AdminIcon :name="card.icon" :size="24" />
        </div>
        <div>
          <p class="admin-stat-card__value">{{ card.value }}</p>
          <p class="admin-stat-card__label">{{ card.label }}</p>
        </div>
      </div>
    </div>

    <div class="admin-panel mb-4" v-if="revenueSummary.totalRevenue != null">
      <div class="admin-panel__head">
        <h2><AdminIcon name="chart" :size="18" /> Doanh thu tháng này</h2>
      </div>
      <div class="admin-panel__body d-flex flex-wrap align-items-center justify-content-between gap-3">
        <div>
          <p class="display-6 fw-bold text-success mb-1">{{ formatMoney(revenueSummary.totalRevenue) }}</p>
          <p class="text-muted mb-0">{{ revenueSummary.orderCount }} đơn hàng · TB {{ formatMoney(revenueSummary.averageOrderValue) }}/đơn</p>
        </div>
        <RouterLink to="/admin/invoices" class="btn btn-primary">
          Xem báo cáo & biểu đồ →
        </RouterLink>
      </div>
    </div>

    <div v-if="!loadingCharts" class="admin-dashboard__kpi-row">
      <div class="admin-dashboard__kpi">
        <div class="admin-dashboard__kpi-label">Doanh thu kỳ</div>
        <div class="admin-dashboard__kpi-value">{{ formatPrice(totalPeriodRevenue) }} ₫</div>
        <div class="admin-dashboard__kpi-sub">{{ charts.revenueByMonth?.length || 0 }} tháng có phát sinh</div>
      </div>
      <div class="admin-dashboard__kpi">
        <div class="admin-dashboard__kpi-label">Đơn hoàn thành</div>
        <div class="admin-dashboard__kpi-value">{{ deliveredOrders }}</div>
        <div class="admin-dashboard__kpi-sub">{{ deliveredPercent }}% tổng đơn</div>
      </div>
      <div class="admin-dashboard__kpi">
        <div class="admin-dashboard__kpi-label">SP bán chạy nhất</div>
        <div class="admin-dashboard__kpi-value dash-kpi-truncate">{{ topProductName }}</div>
        <div class="admin-dashboard__kpi-sub">{{ topProductQty }} sản phẩm đã bán</div>
      </div>
      <div class="admin-dashboard__kpi">
        <div class="admin-dashboard__kpi-label">KH chi tiêu cao</div>
        <div class="admin-dashboard__kpi-value dash-kpi-truncate">{{ topCustomerName }}</div>
        <div class="admin-dashboard__kpi-sub">{{ formatPrice(topCustomerSpend) }} ₫</div>
      </div>
    </div>

    <div v-if="loadingCharts" class="dash-loading">
      <div v-for="n in 3" :key="n" class="dash-skeleton" />
    </div>

    <div v-else class="admin-dashboard__charts">
      <section class="admin-panel">
        <div class="admin-panel__head">
          <h2><AdminIcon name="chart" :size="18" /> Doanh thu theo tháng</h2>
        </div>
        <div class="admin-panel__body">
          <div v-if="!charts.revenueByMonth?.length" class="text-muted">Chưa có dữ liệu.</div>
          <div v-else class="dash-bars">
            <div v-for="item in charts.revenueByMonth" :key="item.month" class="dash-bar-row">
              <span class="dash-bar-row__label">{{ item.label }}</span>
              <div class="dash-bar-row__track">
                <div
                  class="dash-bar-row__fill dash-bar-row__fill--teal"
                  :style="{ width: barPercent(item.revenue, maxRevenue) + '%' }"
                />
              </div>
              <span class="dash-bar-row__value">{{ formatCompact(item.revenue) }}</span>
            </div>
          </div>
        </div>
      </section>

      <section class="admin-panel">
        <div class="admin-panel__head">
          <h2><AdminIcon name="order" :size="18" /> Phân bố trạng thái đơn</h2>
        </div>
        <div class="admin-panel__body dash-donut-wrap">
          <div class="dash-donut" :style="{ background: donutGradient }" />
          <ul class="dash-legend">
            <li v-for="row in charts.orderStatusDistribution" :key="row.status">
              <span class="dash-legend__dot" :style="{ background: statusTheme(row.status).color }" />
              <span>{{ row.label || statusTheme(row.status).label }}</span>
              <strong>{{ row.orderCount }}</strong>
            </li>
          </ul>
        </div>
      </section>

      <section class="admin-panel">
        <div class="admin-panel__head">
          <h2><AdminIcon name="product" :size="18" /> Top sản phẩm bán chạy</h2>
        </div>
        <div class="admin-panel__body">
          <div v-if="!charts.topProducts?.length" class="text-muted">Chưa có dữ liệu.</div>
          <div v-else class="dash-rank-list">
            <div v-for="(item, idx) in charts.topProducts" :key="item.productId" class="dash-rank-item">
              <span class="dash-rank-item__badge" :class="`dash-rank-item__badge--${rankMeta(idx).tone}`">
                {{ rankMeta(idx).badge }}
              </span>
              <div class="dash-rank-item__body">
                <strong class="dash-kpi-truncate">{{ item.productName }}</strong>
                <div class="dash-bar-row__track mt-1">
                  <div
                    class="dash-bar-row__fill dash-bar-row__fill--blue"
                    :style="{ width: barPercent(item.totalQuantity, maxProductQty) + '%' }"
                  />
                </div>
              </div>
              <div class="dash-rank-item__meta">
                <strong>{{ item.totalQuantity }}</strong>
                <span>{{ formatCompact(item.totalRevenue) }} ₫</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="admin-panel">
        <div class="admin-panel__head">
          <h2><AdminIcon name="category" :size="18" /> Doanh thu theo danh mục</h2>
        </div>
        <div class="admin-panel__body">
          <div v-if="!charts.revenueByCategory?.length" class="text-muted">Chưa có dữ liệu.</div>
          <div v-else class="dash-bars">
            <div v-for="(item, idx) in charts.revenueByCategory" :key="item.categoryName" class="dash-bar-row">
              <span class="dash-bar-row__label dash-kpi-truncate">{{ item.categoryName }}</span>
              <div class="dash-bar-row__track">
                <div
                  class="dash-bar-row__fill"
                  :style="{
                    width: barPercent(item.revenue, maxCategoryRevenue) + '%',
                    background: categoryColor(idx),
                  }"
                />
              </div>
              <span class="dash-bar-row__value">{{ formatCompact(item.revenue) }}</span>
            </div>
          </div>
        </div>
      </section>

      <section class="admin-panel admin-panel--wide">
        <div class="admin-panel__head">
          <h2><AdminIcon name="users" :size="18" /> Top khách hàng</h2>
        </div>
        <div class="admin-panel__body">
          <div v-if="!charts.topCustomers?.length" class="text-muted">Chưa có dữ liệu.</div>
          <div v-else class="dash-customer-grid">
            <div v-for="(item, idx) in charts.topCustomers" :key="item.accountId" class="dash-customer-card">
              <div class="dash-customer-card__avatar">{{ customerInitial(item) }}</div>
              <div>
                <strong class="dash-kpi-truncate">{{ item.displayName }}</strong>
                <p class="text-muted mb-0 small">{{ item.orderCount || 0 }} đơn · {{ formatPrice(item.totalSpending) }} ₫</p>
              </div>
              <span class="dash-rank-item__badge" :class="`dash-rank-item__badge--${rankMeta(idx).tone}`">
                {{ rankMeta(idx).badge }}
              </span>
            </div>
          </div>
        </div>
      </section>
    </div>

    <div class="admin-panel mt-4">
      <div class="admin-panel__head">
        <h2>
          <AdminIcon name="settings" :size="18" />
          Truy cập nhanh
        </h2>
      </div>
      <div class="admin-panel__body">
        <div class="admin-quick-links">
          <RouterLink
            v-for="link in quickLinks"
            :key="link.to"
            :to="link.to"
            class="admin-quick-link"
          >
            <span class="admin-quick-link__icon">
              <AdminIcon :name="link.icon" :size="20" />
            </span>
            <span>{{ link.label }}</span>
          </RouterLink>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import AdminLayout from "../../layouts/AdminLayout.vue";
import AdminIcon from "../../components/admin/AdminIcon.vue";
import { filterAdminMenu } from "../../utils/adminAccess";
import { useAppStore } from "../../stores/appStore";
import { useAdminNotify } from "../../utils/adminNotify";
import { apiFetch } from "../../services/http.js";
import {
  barPercent,
  buildConicGradient,
  categoryColor,
  formatCompact,
  formatPrice,
  rankMeta,
  statusTheme,
  sumRevenue,
} from "../../utils/dashboardCharts.js";

const store = useAppStore();
const notify = useAdminNotify();

const err = ref("");
const loadingCharts = ref(true);
const months = ref(12);

const monthOptions = [
  { value: 6, label: "6 tháng" },
  { value: 12, label: "12 tháng" },
  { value: 24, label: "24 tháng" },
];

const stats = ref({
  totalUsers: 0,
  totalProducts: 0,
  totalOrders: 0,
  todayOrders: 0,
  newProducts: 0,
});
const revenueSummary = ref({});

const formatMoney = (v) => `${Number(v || 0).toLocaleString("vi-VN")} ₫`;

const monthRange = () => {
  const now = new Date();
  const from = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().slice(0, 10);
  const to = now.toISOString().slice(0, 10);
  return { from, to };
};

const charts = ref({
  revenueByMonth: [],
  topProducts: [],
  revenueByCategory: [],
  orderStatusDistribution: [],
  topCustomers: [],
});

const statCards = computed(() => [
  { icon: "users", label: "Tài khoản", value: stats.value.totalUsers, tone: "blue" },
  { icon: "product", label: "Sản phẩm", value: stats.value.totalProducts, tone: "green" },
  { icon: "order", label: "Đơn hàng", value: stats.value.totalOrders, tone: "orange" },
  { icon: "chart", label: "Đơn hôm nay", value: stats.value.todayOrders, tone: "purple" },
  { icon: "package", label: "SP mới (7 ngày)", value: stats.value.newProducts, tone: "red" },
]);

const monthsLabel = computed(() => {
  const opt = monthOptions.find((o) => o.value === months.value);
  return opt ? opt.label : `${months.value} tháng`;
});

const maxRevenue = computed(() =>
  Math.max(...(charts.value.revenueByMonth || []).map((i) => Number(i.revenue || 0)), 1)
);

const maxCategoryRevenue = computed(() =>
  Math.max(...(charts.value.revenueByCategory || []).map((i) => Number(i.revenue || 0)), 1)
);

const maxProductQty = computed(() =>
  Math.max(...(charts.value.topProducts || []).map((i) => Number(i.totalQuantity || 0)), 1)
);

const totalPeriodRevenue = computed(() => sumRevenue(charts.value.revenueByMonth));

const totalOrdersInChart = computed(() =>
  (charts.value.orderStatusDistribution || []).reduce((acc, i) => acc + Number(i.orderCount || 0), 0)
);

const deliveredOrders = computed(() => {
  const row = (charts.value.orderStatusDistribution || []).find((i) => i.status === "DELIVERED");
  return row?.orderCount ?? 0;
});

const deliveredPercent = computed(() => {
  const total = totalOrdersInChart.value;
  if (!total) return 0;
  return Math.round((deliveredOrders.value / total) * 100);
});

const topProductName = computed(() => charts.value.topProducts?.[0]?.productName || "—");
const topProductQty = computed(() => charts.value.topProducts?.[0]?.totalQuantity ?? 0);
const topCustomerName = computed(() => charts.value.topCustomers?.[0]?.displayName || "—");
const topCustomerSpend = computed(() => charts.value.topCustomers?.[0]?.totalSpending ?? 0);

const donutGradient = computed(() => buildConicGradient(charts.value.orderStatusDistribution));

const quickLinks = computed(() =>
  filterAdminMenu(store.state.user).map((item) => ({
    to: item.to,
    label: `Quản lý ${item.label.toLowerCase()}`,
    icon: item.icon,
  }))
);

const customerInitial = (item) => {
  const name = item.displayName || item.username || "?";
  return name.charAt(0).toUpperCase();
};

const loadDashboard = async () => {
  loadingCharts.value = true;
  err.value = "";
  try {
    const { from, to } = monthRange();
    const [data, rev] = await Promise.all([
      apiFetch(`/api/admin/dashboard?months=${months.value}`),
      apiFetch(`/api/admin/reports/summary?from=${from}&to=${to}`).catch(() => ({})),
    ]);
    stats.value = {
      totalUsers: data.totalUsers ?? 0,
      totalProducts: data.totalProducts ?? 0,
      totalOrders: data.totalOrders ?? 0,
      todayOrders: data.todayOrders ?? 0,
      newProducts: data.newProducts ?? 0,
    };
    charts.value = data.charts || charts.value;
    revenueSummary.value = rev || {};
  } catch (e) {
    const msg = e?.message || "Không tải được thống kê dashboard.";
    err.value = msg;
    notify.error(msg);
  } finally {
    loadingCharts.value = false;
  }
};

const changeMonths = async (value) => {
  if (months.value === value) return;
  months.value = value;
  await loadDashboard();
};

onMounted(loadDashboard);
</script>
