<template>
  <AdminLayout>
    <div class="rank-admin">
      <div class="rank-admin__head">
        <div>
          <h1 class="rank-admin__title">Hạng thành viên</h1>
          <p class="rank-admin__subtitle">
            Quản lý bậc hạng theo điểm tích lũy — tự nâng khi đơn <strong>Đã giao</strong> / đánh giá.
          </p>
        </div>
        <button v-if="canWrite" type="button" class="rank-admin__btn-primary" @click="openCreate">
          + Thêm hạng
        </button>
      </div>

      <div v-if="flashOk" class="alert alert-success">{{ flashOk }}</div>
      <div v-if="flashErr" class="alert alert-danger">{{ flashErr }}</div>

      <div class="rank-admin__rules">
        <span>Đơn đã giao: <strong>1 điểm / 1.000đ</strong></span>
        <span>Đánh giá SP: <strong>+20 điểm</strong></span>
        <span>Hạng tự cập nhật theo <strong>MinPoint</strong></span>
      </div>

      <div class="rank-admin__layout">
        <div class="rank-admin__list">
          <p v-if="loading" class="rank-admin__empty">Đang tải...</p>
          <p v-else-if="!sortedRanks.length" class="rank-admin__empty">Chưa có hạng</p>
          <article
            v-for="(r, idx) in sortedRanks"
            :key="r.id"
            class="rank-card"
            :class="[`rank-card--${rankVisual(r.rankName).tone}`, { 'is-off': r.isActive === false }]"
          >
            <div class="rank-card__medal" aria-hidden="true">
              <span>{{ rankVisual(r.rankName).icon }}</span>
              <small>#{{ idx + 1 }}</small>
            </div>
            <div class="rank-card__body">
              <div class="rank-card__title-row">
                <h3>{{ r.rankName }}</h3>
                <span v-if="r.isActive !== false" class="rank-card__status is-on">Đang dùng</span>
                <span v-else class="rank-card__status">Tắt</span>
              </div>
              <p v-if="r.description" class="rank-card__desc">{{ r.description }}</p>
              <div class="rank-card__meta">
                <div>
                  <span>Điểm tối thiểu</span>
                  <strong>{{ formatNum(r.minPoint) }}</strong>
                </div>
                <div>
                  <span>Ưu đãi</span>
                  <strong>{{ formatDiscount(r.discountPercent) }}%</strong>
                </div>
                <div>
                  <span>Thành viên</span>
                  <strong>{{ formatNum(r.memberCount) }}</strong>
                </div>
              </div>
            </div>
            <div v-if="canWrite" class="rank-card__actions">
              <button type="button" class="rank-card__btn" @click="openEdit(r)">Sửa</button>
              <button
                type="button"
                class="rank-card__btn rank-card__btn--danger"
                :disabled="r.id === 1 || (r.memberCount || 0) > 0"
                @click="remove(r)"
              >
                Xóa
              </button>
            </div>
          </article>
        </div>

        <aside v-if="canWrite && showForm" class="rank-admin__form-wrap">
          <div class="rank-admin__form">
            <div class="rank-admin__form-head">
              <strong>{{ editingId ? "Cập nhật hạng" : "Thêm hạng mới" }}</strong>
              <button type="button" class="rank-admin__form-close" @click="closeForm">Đóng</button>
            </div>
            <form @submit.prevent="save">
              <label class="rank-admin__field">
                <span>Tên hạng <em>*</em></span>
                <input
                  v-model="form.rankName"
                  type="text"
                  maxlength="50"
                  :class="{ 'is-invalid': fieldErrors.rankName }"
                  @input="delete fieldErrors.rankName"
                />
                <small v-if="fieldErrors.rankName">{{ fieldErrors.rankName }}</small>
              </label>
              <label class="rank-admin__field">
                <span>Điểm tối thiểu <em>*</em></span>
                <input
                  v-model.number="form.minPoint"
                  type="number"
                  min="0"
                  :class="{ 'is-invalid': fieldErrors.minPoint }"
                  @input="delete fieldErrors.minPoint"
                />
                <small v-if="fieldErrors.minPoint">{{ fieldErrors.minPoint }}</small>
              </label>
              <label class="rank-admin__field">
                <span>Giảm giá (%) <em>*</em></span>
                <input
                  v-model.number="form.discountPercent"
                  type="number"
                  min="0"
                  max="100"
                  step="0.01"
                  :class="{ 'is-invalid': fieldErrors.discountPercent }"
                  @input="delete fieldErrors.discountPercent"
                />
                <small v-if="fieldErrors.discountPercent">{{ fieldErrors.discountPercent }}</small>
              </label>
              <label class="rank-admin__field">
                <span>Mô tả</span>
                <textarea
                  v-model="form.description"
                  rows="3"
                  maxlength="255"
                  :class="{ 'is-invalid': fieldErrors.description }"
                  @input="delete fieldErrors.description"
                />
                <small v-if="fieldErrors.description">{{ fieldErrors.description }}</small>
              </label>
              <label class="rank-admin__check">
                <input v-model="form.isActive" type="checkbox" />
                Đang kích hoạt
              </label>
              <button type="submit" class="rank-admin__btn-primary rank-admin__btn-primary--block" :disabled="saving">
                {{ saving ? "Đang lưu..." : "Lưu hạng" }}
              </button>
            </form>
          </div>
        </aside>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import AdminLayout from "../../layouts/AdminLayout.vue";
import { apiFetch } from "../../services/http.js";
import { useAppStore, useToast } from "../../stores/appStore";
import { userCanWriteCatalog } from "../../utils/adminAccess";
import { rankVisual } from "../../utils/rankStyle";
import { firstError, getApiError, runValidation } from "../../utils/validators";

const store = useAppStore();
const toast = useToast();
const canWrite = computed(() => userCanWriteCatalog(store.state.user));

const ranks = ref([]);
const loading = ref(false);
const saving = ref(false);
const showForm = ref(false);
const editingId = ref(null);
const flashOk = ref("");
const flashErr = ref("");
const fieldErrors = reactive({});

const form = reactive({
  rankName: "",
  minPoint: null,
  discountPercent: null,
  description: "",
  isActive: true,
});

const sortedRanks = computed(() =>
  [...ranks.value].sort((a, b) => (a.minPoint ?? 0) - (b.minPoint ?? 0))
);

const formatDiscount = (v) => {
  if (v == null || v === "") return "0";
  return Number(v);
};

const formatNum = (v) => {
  const n = Number(v ?? 0);
  return Number.isFinite(n) ? n.toLocaleString("vi-VN") : "0";
};

const resetForm = () => {
  editingId.value = null;
  form.rankName = "";
  form.minPoint = null;
  form.discountPercent = null;
  form.description = "";
  form.isActive = true;
  Object.keys(fieldErrors).forEach((k) => delete fieldErrors[k]);
};

const openCreate = () => {
  resetForm();
  showForm.value = true;
};

const openEdit = (r) => {
  editingId.value = r.id;
  form.rankName = r.rankName || "";
  form.minPoint = r.minPoint ?? 0;
  form.discountPercent = Number(r.discountPercent ?? 0);
  form.description = r.description || "";
  form.isActive = r.isActive !== false;
  showForm.value = true;
};

const closeForm = () => {
  showForm.value = false;
  resetForm();
};

const load = async () => {
  loading.value = true;
  flashErr.value = "";
  try {
    const data = await apiFetch("/api/admin/ranks");
    ranks.value = data.ranks || [];
  } catch (e) {
    flashErr.value = e?.message || "Không tải được danh sách hạng.";
    ranks.value = [];
  } finally {
    loading.value = false;
  }
};

const save = async () => {
  flashOk.value = "";
  flashErr.value = "";
  Object.keys(fieldErrors).forEach((k) => delete fieldErrors[k]);
  const result = runValidation(
    {
      rankName: form.rankName,
      minPoint: form.minPoint,
      discountPercent: form.discountPercent,
      description: form.description,
    },
    {
      rankName: [
        "required",
        { type: "min", min: 2, message: "Tên hạng tối thiểu 2 ký tự." },
        { type: "max", max: 50 },
      ],
      minPoint: [
        { type: "required", message: "Điểm tối thiểu là bắt buộc." },
        { type: "number", min: 0, max: 1000000, message: "Điểm tối thiểu ≥ 0." },
      ],
      discountPercent: [
        { type: "required", message: "Giảm giá % là bắt buộc." },
        { type: "number", min: 0, max: 100, message: "Giảm giá từ 0 đến 100%." },
      ],
      description: [{ type: "max", max: 255 }],
    }
  );
  if (!result.ok) {
    Object.assign(fieldErrors, result.errors);
    flashErr.value = firstError(result.errors);
    toast.error(flashErr.value);
    return;
  }

  saving.value = true;
  const payload = {
    rankName: String(result.values.rankName),
    minPoint: Number(result.values.minPoint),
    discountPercent: Number(result.values.discountPercent),
    description: String(form.description || "").trim() || null,
    isActive: !!form.isActive,
  };
  try {
    const url = editingId.value ? `/api/admin/ranks/${editingId.value}` : "/api/admin/ranks";
    const method = editingId.value ? "PUT" : "POST";
    const data = await apiFetch(url, { method, body: JSON.stringify(payload) });
    flashOk.value = data.message || "Đã lưu.";
    toast.success(flashOk.value);
    closeForm();
    await load();
  } catch (e) {
    const api = getApiError(e, "Lưu thất bại.");
    Object.assign(fieldErrors, api.errors || {});
    flashErr.value = api.message;
    toast.error(flashErr.value);
  } finally {
    saving.value = false;
  }
};

const remove = async (r) => {
  if (!confirm(`Xóa hạng "${r.rankName}"?`)) return;
  flashOk.value = "";
  flashErr.value = "";
  try {
    const data = await apiFetch(`/api/admin/ranks/${r.id}`, { method: "DELETE" });
    flashOk.value = data.message || "Đã xóa.";
    toast.success(flashOk.value);
    await load();
  } catch (e) {
    flashErr.value = e?.message || "Xóa thất bại.";
    toast.error(flashErr.value);
  }
};

onMounted(load);
</script>
