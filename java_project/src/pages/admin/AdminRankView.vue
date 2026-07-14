<template>
  <AdminLayout>
    <div class="container-fluid px-4 mt-4">
      <div class="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h4 class="fw-bold mb-1">Hạng thành viên</h4>
          <p class="text-muted mb-0">
            Quản lý hạng gắn với người mua (điểm từ đơn giao &amp; đánh giá)
          </p>
        </div>
        <button v-if="canWrite" type="button" class="btn btn-primary" @click="openCreate">
          + Thêm hạng
        </button>
      </div>

      <div v-if="flashOk" class="alert alert-success">{{ flashOk }}</div>
      <div v-if="flashErr" class="alert alert-danger">{{ flashErr }}</div>

      <div class="row g-4">
        <div class="col-lg-8">
          <div class="card shadow border-0">
            <div class="card-header bg-dark text-white fw-semibold">
              Danh sách hạng ({{ ranks.length }})
            </div>
            <div class="card-body p-0">
              <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                  <thead class="table-light">
                    <tr>
                      <th style="width: 60px">ID</th>
                      <th>Tên hạng</th>
                      <th>Điểm tối thiểu</th>
                      <th>Giảm giá %</th>
                      <th>Thành viên</th>
                      <th>Trạng thái</th>
                      <th v-if="canWrite" style="width: 160px">Hành động</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-if="!loading && !ranks.length">
                      <td :colspan="canWrite ? 7 : 6" class="text-center text-muted py-4">
                        Chưa có hạng
                      </td>
                    </tr>
                    <tr v-for="r in ranks" :key="r.id">
                      <td>{{ r.id }}</td>
                      <td>
                        <div class="fw-semibold">{{ r.rankName }}</div>
                        <small class="text-muted">{{ r.description || "" }}</small>
                      </td>
                      <td>{{ r.minPoint ?? 0 }}</td>
                      <td>{{ formatDiscount(r.discountPercent) }}%</td>
                      <td>
                        <span class="badge text-bg-secondary">{{ r.memberCount ?? 0 }}</span>
                      </td>
                      <td>
                        <span v-if="r.isActive !== false" class="badge bg-success">Đang dùng</span>
                        <span v-else class="badge bg-secondary">Tắt</span>
                      </td>
                      <td v-if="canWrite">
                        <button type="button" class="btn btn-sm btn-warning me-1" @click="openEdit(r)">
                          Sửa
                        </button>
                        <button
                          type="button"
                          class="btn btn-sm btn-outline-danger"
                          :disabled="r.id === 1 || (r.memberCount || 0) > 0"
                          @click="remove(r)"
                        >
                          Xóa
                        </button>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
          <p class="text-muted small mt-2 mb-0">
            Quy tắc điểm: đơn <strong>Đã giao</strong> = 1 điểm / 1.000đ; mỗi đánh giá sản phẩm = +20 điểm.
            Hạng tự nâng theo MinPoint.
          </p>
        </div>

        <div v-if="canWrite && showForm" class="col-lg-4">
          <div class="card shadow border-0">
            <div class="card-header bg-primary text-white d-flex justify-content-between align-items-center">
              <strong>{{ editingId ? "Cập nhật hạng" : "Thêm hạng mới" }}</strong>
              <button type="button" class="btn btn-sm btn-light" @click="closeForm">Đóng</button>
            </div>
            <div class="card-body">
              <form @submit.prevent="save">
                <div class="mb-3">
                  <label class="form-label">Tên hạng <span class="text-danger">*</span></label>
                  <input
                    v-model="form.rankName"
                    type="text"
                    class="form-control"
                    :class="{ 'is-invalid': fieldErrors.rankName }"
                    maxlength="50"
                    @input="delete fieldErrors.rankName"
                  />
                  <div v-if="fieldErrors.rankName" class="invalid-feedback d-block">{{ fieldErrors.rankName }}</div>
                </div>
                <div class="mb-3">
                  <label class="form-label">Điểm tối thiểu <span class="text-danger">*</span></label>
                  <input
                    v-model.number="form.minPoint"
                    type="number"
                    class="form-control"
                    :class="{ 'is-invalid': fieldErrors.minPoint }"
                    min="0"
                    @input="delete fieldErrors.minPoint"
                  />
                  <div v-if="fieldErrors.minPoint" class="invalid-feedback d-block">{{ fieldErrors.minPoint }}</div>
                </div>
                <div class="mb-3">
                  <label class="form-label">Giảm giá (%) <span class="text-danger">*</span></label>
                  <input
                    v-model.number="form.discountPercent"
                    type="number"
                    class="form-control"
                    :class="{ 'is-invalid': fieldErrors.discountPercent }"
                    min="0"
                    max="100"
                    step="0.01"
                    @input="delete fieldErrors.discountPercent"
                  />
                  <div v-if="fieldErrors.discountPercent" class="invalid-feedback d-block">{{ fieldErrors.discountPercent }}</div>
                </div>
                <div class="mb-3">
                  <label class="form-label">Mô tả</label>
                  <textarea
                    v-model="form.description"
                    class="form-control"
                    :class="{ 'is-invalid': fieldErrors.description }"
                    rows="2"
                    maxlength="255"
                    @input="delete fieldErrors.description"
                  />
                  <div v-if="fieldErrors.description" class="invalid-feedback d-block">{{ fieldErrors.description }}</div>
                </div>
                <div class="form-check mb-3">
                  <input id="rank-active" v-model="form.isActive" class="form-check-input" type="checkbox" />
                  <label class="form-check-label" for="rank-active">Đang kích hoạt</label>
                </div>
                <button type="submit" class="btn btn-success w-100" :disabled="saving">
                  {{ saving ? "Đang lưu..." : "Lưu" }}
                </button>
              </form>
            </div>
          </div>
        </div>
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

const formatDiscount = (v) => {
  if (v == null || v === "") return "0";
  return Number(v);
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
