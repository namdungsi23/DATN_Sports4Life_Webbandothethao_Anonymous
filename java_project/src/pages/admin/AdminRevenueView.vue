<template>
  <AdminLayout>
    <div class="admin-dashboard__toolbar">
      <div>
        <p>Báo cáo doanh thu Sports4Life — {{ monthsLabel }}.</p>
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

    <div v-if="loading" class="dash-loading">
      <div v-for="n in 3" :key="n" class="dash-skeleton" />
    </div>

    <template v-else>
      <div class="admin-dashboard__kpi-row">
        <div class="admin-dashboard__kpi">
          <div class="admin-dashboard__kpi-label">Tổng doanh thu</div>
          <div class="admin-dashboard__kpi-value">{{ formatPrice(totalRevenue) }} ₫</div>
          <div class="admin-dashboard__kpi-sub">{{ charts.revenueByMonth?.length || 0 }} tháng có phát sinh</div>
        </div>
        <div class="admin-dashboard__kpi">
          <div class="admin-dashboard__kpi-label">Tháng cao nhất</div>
          <div class="admin-dashboard__kpi-value dash-kpi-truncate">{{ bestMonth?.label || "—" }}</div>
          <div class="admin-dashboard__kpi-sub">{{ formatPrice(bestMonth?.revenue) }} ₫</div>
        </div>
        <div class="admin-dashboard__kpi">
          <div class="admin-dashboard__kpi-label">Danh mục dẫn đầu</div>
          <div class="admin-dashboard__kpi-value dash-kpi-truncate">{{ topCategory?.categoryName || "—" }}</div>
          <div class="admin-dashboard__kpi-sub">{{ formatPrice(topCategory?.revenue) }} ₫</div>
        </div>
        <div class="admin-dashboard__kpi">
          <div class="admin-dashboard__kpi-label">SP doanh thu cao nhất</div>
          <div class="admin-dashboard__kpi-value dash-kpi-truncate">{{ topProduct?.productName || "—" }}</div>
          <div class="admin-dashboard__kpi-sub">{{ formatPrice(topProduct?.totalRevenue) }} ₫</div>
        </div>
      </div>

      <div class="admin-dashboard__charts">
        <section class="admin-panel admin-panel--wide">
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
                <span class="dash-bar-row__value">{{ formatPrice(item.revenue) }} ₫</span>
              </div>
            </div>
          </div>
        </section>

        <section class="admin-panel">
          <div class="admin-panel__head">
            <h2><AdminIcon name="category" :size="18" /> Theo danh mục</h2>
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

        <section class="admin-panel">
          <div class="admin-panel__head">
            <h2><AdminIcon name="product" :size="18" /> Top SP theo doanh thu</h2>
          </div>
          <div class="admin-panel__body">
            <div v-if="!topProductsByRevenue.length" class="text-muted">Chưa có dữ liệu.</div>
            <div v-else class="dash-rank-list">
              <div
                v-for="(item, idx) in topProductsByRevenue"
                :key="item.productId"
                class="dash-rank-item"
              >
                <span class="dash-rank-item__badge" :class="`dash-rank-item__badge--${rankMeta(idx).tone}`">
                  {{ rankMeta(idx).badge }}
                </span>
                <div class="dash-rank-item__body">
                  <strong class="dash-kpi-truncate">{{ item.productName }}</strong>
                  <p class="mb-0 small text-muted">{{ item.totalQuantity }} sản phẩm · {{ revenueShare(item.totalRevenue) }}%</p>
                </div>
                <strong>{{ formatCompact(item.totalRevenue) }} ₫</strong>
              </div>
            </div>
          </div>
        </section>
      </div>
    </template>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import AdminLayout from "../../layouts/AdminLayout.vue";
import AdminIcon from "../../components/admin/AdminIcon.vue";
import { apiFetch } from "../../services/http.js";
import { useAdminNotify } from "../../utils/adminNotify";
import {
  barPercent,
  categoryColor,
  formatCompact,
  formatPrice,
  rankMeta,
  sumRevenue,
} from "../../utils/dashboardCharts.js";

const notify = useAdminNotify();
const err = ref("");
const loading = ref(true);
const months = ref(12);

const monthOptions = [
  { value: 6, label: "6 tháng" },
  { value: 12, label: "12 tháng" },
  { value: 24, label: "24 tháng" },
];

const charts = ref({
  revenueByMonth: [],
  revenueByCategory: [],
  topProducts: [],
});

const monthsLabel = computed(() => {
  const opt = monthOptions.find((o) => o.value === months.value);
  return opt ? opt.label : `${months.value} tháng`;
});

const totalRevenue = computed(() => sumRevenue(charts.value.revenueByMonth));

const maxRevenue = computed(() =>
  Math.max(...(charts.value.revenueByMonth || []).map((i) => Number(i.revenue || 0)), 1)
);

const maxCategoryRevenue = computed(() =>
  Math.max(...(charts.value.revenueByCategory || []).map((i) => Number(i.revenue || 0)), 1)
);

const bestMonth = computed(() => {
  const items = charts.value.revenueByMonth || [];
  if (!items.length) return null;
  return items.reduce((best, item) =>
    Number(item.revenue || 0) > Number(best.revenue || 0) ? item : best
  );
});

const topCategory = computed(() => charts.value.revenueByCategory?.[0] || null);

const topProductsByRevenue = computed(() =>
  [...(charts.value.topProducts || [])].sort(
    (a, b) => Number(b.totalRevenue || 0) - Number(a.totalRevenue || 0)
  )
);

const topProduct = computed(() => topProductsByRevenue.value[0] || null);

const totalProductRevenue = computed(() => sumRevenue(topProductsByRevenue.value, "totalRevenue"));

const revenueShare = (value) => {
  const total = totalProductRevenue.value;
  if (!total) return 0;
  return Math.round((Number(value || 0) / total) * 1000) / 10;
};

const loadRevenue = async () => {
  loading.value = true;
  err.value = "";
  try {
    const data = await apiFetch(`/api/admin/dashboard/charts?months=${months.value}`);
    charts.value = {
      revenueByMonth: data.revenueByMonth || [],
      revenueByCategory: data.revenueByCategory || [],
      topProducts: data.topProducts || [],
    };
  } catch (e) {
    const msg = e?.message || "Không tải được báo cáo doanh thu.";
    err.value = msg;
    notify.error(msg);
  } finally {
    loading.value = false;
  }
};

const changeMonths = async (value) => {
  if (months.value === value) return;
  months.value = value;
  await loadRevenue();
};

onMounted(loadRevenue);
</script>
