<template>
  <AdminLayout>
    <AdminReadOnlyNotice />
    <div class="container-fluid px-4 mt-4">
      <div class="d-flex justify-content-between align-items-center mb-4">
        <h4 class="fw-bold mb-0">Quản lý mã khuyến mãi</h4>
      </div>

      <div v-if="flashOk" class="alert alert-success">{{ flashOk }}</div>
      <div v-if="flashErr" class="alert alert-danger">{{ flashErr }}</div>
      <div v-if="err" class="alert alert-danger">{{ err }}</div>

      <form class="row g-2 mb-3" @submit.prevent="search">
        <div class="col-md-6">
          <input v-model="keyword" type="text" class="form-control" placeholder="Tìm theo mã hoặc tên" />
        </div>
        <div class="col-md-3">
          <button type="submit" class="btn btn-primary w-100">Tìm kiếm</button>
        </div>
      </form>

      <div class="row g-4">
        <div class="col-lg-5">
          <div class="card shadow border-0">
            <div class="card-header" :class="form.id ? 'bg-warning text-dark' : 'bg-primary text-white'">
              <strong>{{ form.id ? "Cập nhật mã" : "Tạo mã khuyến mãi" }}</strong>
            </div>
            <div class="card-body">
              <fieldset :disabled="!canWrite">
                <form @submit.prevent="saveVoucher">
                  <div class="mb-3">
                    <label class="form-label fw-semibold">Mã khuyến mãi</label>
                    <input v-model="form.code" type="text" class="form-control text-uppercase" placeholder="VD: SUMMER50K" required />
                  </div>
                  <div class="mb-3">
                    <label class="form-label fw-semibold">Tên hiển thị</label>
                    <input v-model="form.name" type="text" class="form-control" placeholder="Mô tả ngắn" />
                  </div>
                  <div class="mb-3">
                    <label class="form-label fw-semibold">Loại giảm giá</label>
                    <select v-model="form.discountType" class="form-select">
                      <option value="PERCENT">Phần trăm (%)</option>
                      <option value="FIXED">Số tiền cố định</option>
                      <option value="FREESHIP">Miễn phí vận chuyển</option>
                    </select>
                  </div>
                  <div v-if="form.discountType === 'PERCENT'" class="mb-3">
                    <label class="form-label fw-semibold">Giảm (%)</label>
                    <input v-model.number="form.discountPercent" type="number" min="1" max="100" class="form-control" required />
                  </div>
                  <div v-else class="mb-3">
                    <label class="form-label fw-semibold">Giảm (VNĐ)</label>
                    <input v-model.number="form.discountAmount" type="number" min="1000" max="99999999" step="1000" class="form-control" required />
                  </div>
                  <div v-if="form.discountType === 'PERCENT'" class="mb-3">
                    <label class="form-label fw-semibold">Giảm tối đa (VNĐ)</label>
                    <input v-model.number="form.maxDiscount" type="number" min="0" max="99999999" step="1000" class="form-control" />
                  </div>
                  <div class="mb-3">
                    <label class="form-label fw-semibold">Đơn tối thiểu (VNĐ)</label>
                    <input v-model.number="form.minOrderValue" type="number" min="0" max="99999999" step="10000" class="form-control" />
                  </div>
                  <div class="mb-3">
                    <label class="form-label fw-semibold">Số lượt sử dụng tối đa</label>
                    <input v-model.number="form.quantity" type="number" min="1" class="form-control" required />
                  </div>
                  <div class="row g-2 mb-3">
                    <div class="col-md-6">
                      <label class="form-label fw-semibold">Bắt đầu</label>
                      <input v-model="form.startDateLocal" type="datetime-local" class="form-control" required />
                    </div>
                    <div class="col-md-6">
                      <label class="form-label fw-semibold">Kết thúc</label>
                      <input v-model="form.expiredAtLocal" type="datetime-local" class="form-control" required />
                    </div>
                  </div>
                  <div class="form-check form-switch mb-3">
                    <input id="voucher-active" v-model="form.active" class="form-check-input" type="checkbox" />
                    <label class="form-check-label" for="voucher-active">Đang bật</label>
                  </div>
                  <button type="submit" class="btn btn-success w-100 fw-semibold">Lưu mã</button>
                  <button v-if="form.id" type="button" class="btn btn-outline-secondary w-100 mt-2" @click="resetForm">Làm mới</button>
                </form>
              </fieldset>
            </div>
          </div>
        </div>

        <div class="col-lg-7">
          <div class="card shadow border-0">
            <div class="card-header bg-dark text-white fw-semibold">Danh sách mã khuyến mãi</div>
            <div class="card-body p-0">
              <table class="table table-hover align-middle mb-0">
                <thead class="table-light">
                  <tr>
                    <th>Mã</th>
                    <th>Loại</th>
                    <th>Giá trị</th>
                    <th>Đã dùng</th>
                    <th>Trạng thái</th>
                    <th>Hành động</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="v in vouchers" :key="v.id">
                    <td>
                      <strong>{{ v.code }}</strong>
                      <div class="small text-muted">{{ v.name }}</div>
                    </td>
                    <td>{{ discountTypeLabel(v) }}</td>
                    <td>{{ discountValueLabel(v) }}</td>
                    <td>{{ v.usedCount ?? 0 }} / {{ v.quantity ?? 0 }}</td>
                    <td>
                      <span class="badge" :class="v.isActive === 1 ? 'bg-success' : 'bg-secondary'">
                        {{ v.isActive === 1 ? "Bật" : "Tắt" }}
                      </span>
                    </td>
                    <td class="text-nowrap">
                      <template v-if="canWrite">
                        <button type="button" class="btn btn-sm btn-outline-warning me-1" @click="editRow(v)">Sửa</button>
                        <button type="button" class="btn btn-sm btn-outline-primary me-1" @click="toggleRow(v)">
                          {{ v.isActive === 1 ? "Tắt" : "Bật" }}
                        </button>
                        <button type="button" class="btn btn-sm btn-outline-danger" @click="remove(v.id)">Xóa</button>
                      </template>
                      <span v-else class="text-muted small">Chỉ xem</span>
                    </td>
                  </tr>
                  <tr v-if="!vouchers.length">
                    <td colspan="6" class="text-center text-muted py-4">Chưa có mã khuyến mãi</td>
                  </tr>
                </tbody>
              </table>
              <nav v-if="totalPages > 1" class="p-3">
                <ul class="pagination justify-content-center mb-0">
                  <li class="page-item" :class="{ disabled: page === 0 }">
                    <a class="page-link" href="#" @click.prevent="goPage(page - 1)">‹</a>
                  </li>
                  <li v-for="i in totalPages" :key="i" class="page-item" :class="{ active: i - 1 === page }">
                    <a class="page-link" href="#" @click.prevent="goPage(i - 1)">{{ i }}</a>
                  </li>
                  <li class="page-item" :class="{ disabled: page + 1 >= totalPages }">
                    <a class="page-link" href="#" @click.prevent="goPage(page + 1)">›</a>
                  </li>
                </ul>
              </nav>
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
import AdminReadOnlyNotice from "../../components/admin/AdminReadOnlyNotice.vue";
import { apiFetch } from "../../services/http.js";
import { useAppStore } from "../../stores/appStore";
import { userCanWriteVouchers } from "../../utils/adminAccess";

const store = useAppStore();
const canWrite = computed(() => userCanWriteVouchers(store.state.user));

const err = ref("");
const flashOk = ref("");
const flashErr = ref("");
const keyword = ref("");
const vouchers = ref([]);
const page = ref(0);
const totalPages = ref(1);

const emptyForm = () => ({
  id: null,
  code: "",
  name: "",
  discountType: "PERCENT",
  discountPercent: 10,
  discountAmount: 50000,
  minOrderValue: 200000,
  maxDiscount: 100000,
  quantity: 100,
  startDateLocal: "",
  expiredAtLocal: "",
  active: true,
});

const form = reactive(emptyForm());

function toLocalInput(iso) {
  if (!iso) return "";
  const d = new Date(iso);
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function toInstant(local) {
  return local ? new Date(local).toISOString() : null;
}

function discountTypeLabel(v) {
  if (v.code === "FREESHIP" || (v.name || "").toLowerCase().includes("miễn phí")) return "Freeship";
  if (v.discountPercent) return "Phần trăm";
  return "Cố định";
}

function discountValueLabel(v) {
  if (v.discountPercent) return `${v.discountPercent}%`;
  if (v.discountAmount) return `${Number(v.discountAmount).toLocaleString("vi-VN")}đ`;
  return "—";
}

async function loadList() {
  err.value = "";
  const q = keyword.value ? `&keyword=${encodeURIComponent(keyword.value)}` : "";
  const data = await apiFetch(`/api/admin/vouchers?page=${page.value}&size=12${q}`);
  vouchers.value = data.items || [];
  totalPages.value = data.totalPages || 1;
}

function search() {
  page.value = 0;
  loadList().catch((e) => { err.value = e.message; });
}

function goPage(p) {
  if (p < 0 || p >= totalPages.value) return;
  page.value = p;
  loadList().catch((e) => { err.value = e.message; });
}

function resetForm() {
  Object.assign(form, emptyForm());
}

function editRow(v) {
  Object.assign(form, {
    id: v.id,
    code: v.code,
    name: v.name || "",
    discountType: v.code === "FREESHIP" ? "FREESHIP" : v.discountPercent ? "PERCENT" : "FIXED",
    discountPercent: v.discountPercent || 10,
    discountAmount: Number(v.discountAmount || 50000),
    minOrderValue: Number(v.minOrderValue || 0),
    maxDiscount: Number(v.maxDiscount || 0),
    quantity: v.quantity || 100,
    startDateLocal: toLocalInput(v.startDate),
    expiredAtLocal: toLocalInput(v.expiredAt),
    active: v.isActive === 1,
  });
}

function buildPayload() {
  const discountType = form.discountType;
  const minOrder = Math.min(99999999, Math.max(0, Math.round(Number(form.minOrderValue) || 0)));
  const quantity = Math.max(1, Math.round(Number(form.quantity) || 1));

  const payload = {
    code: form.code.trim().toUpperCase(),
    name: form.name,
    discountType,
    minOrderValue: minOrder,
    quantity,
    startDate: toInstant(form.startDateLocal),
    expiredAt: toInstant(form.expiredAtLocal),
    isActive: form.active ? 1 : 0,
  };

  if (discountType === "PERCENT") {
    payload.discountPercent = Math.min(100, Math.max(1, Math.round(Number(form.discountPercent) || 0)));
    payload.discountAmount = null;
    const maxDisc = Math.round(Number(form.maxDiscount) || 0);
    payload.maxDiscount = maxDisc > 0 ? Math.min(99999999, maxDisc) : null;
  } else {
    const amount = Math.min(99999999, Math.max(1000, Math.round(Number(form.discountAmount) || 0)));
    payload.discountPercent = null;
    payload.discountAmount = amount;
    payload.maxDiscount = amount;
  }

  return payload;
}

async function saveVoucher() {
  if (!canWrite.value) return;
  flashOk.value = "";
  flashErr.value = "";
  try {
    const payload = buildPayload();
    if (form.id) {
      await apiFetch(`/api/admin/vouchers/${form.id}`, { method: "PUT", body: JSON.stringify(payload) });
      flashOk.value = "Cập nhật mã thành công";
    } else {
      await apiFetch("/api/admin/vouchers", { method: "POST", body: JSON.stringify(payload) });
      flashOk.value = "Tạo mã thành công";
    }
    resetForm();
    await loadList();
  } catch (e) {
    flashErr.value = e.message || "Lưu thất bại";
  }
}

async function toggleRow(v) {
  if (!canWrite.value) return;
  try {
    await apiFetch(`/api/admin/vouchers/${v.id}/toggle?active=${v.isActive !== 1}`, { method: "PUT" });
    await loadList();
  } catch (e) {
    flashErr.value = e.message || "Không thể đổi trạng thái";
  }
}

async function remove(id) {
  if (!canWrite.value || !confirm("Xóa mã khuyến mãi này?")) return;
  try {
    await apiFetch(`/api/admin/vouchers/${id}`, { method: "DELETE" });
    flashOk.value = "Đã xóa mã";
    await loadList();
  } catch (e) {
    flashErr.value = e.message || "Xóa thất bại";
  }
}

onMounted(() => {
  const now = new Date();
  const nextMonth = new Date(now);
  nextMonth.setMonth(nextMonth.getMonth() + 1);
  form.startDateLocal = toLocalInput(now.toISOString());
  form.expiredAtLocal = toLocalInput(nextMonth.toISOString());
  loadList().catch((e) => { err.value = e.message; });
});
</script>
