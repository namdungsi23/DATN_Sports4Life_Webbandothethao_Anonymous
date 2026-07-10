<template>
  <MainLayout>
    <div class="container my-4 address-book">
      <div class="address-book__head">
        <div>
          <h2 class="mb-1">Sổ địa chỉ</h2>
          <p class="text-muted mb-0">Quản lý địa chỉ giao hàng đã lưu. Họ tên và số điện thoại được lấy từ tài khoản của bạn.</p>
        </div>
        <button type="button" class="btn btn-primary" @click="openCreateForm">
          + Thêm địa chỉ mới
        </button>
      </div>

      <div v-if="accountFullName || accountPhone" class="alert alert-light border py-2 mb-3">
        <strong>Người nhận (từ tài khoản):</strong>
        {{ accountFullName || "—" }}
        <span v-if="accountPhone"> · {{ accountPhone }}</span>
        <RouterLink to="/profile" class="ms-2 small">Cập nhật hồ sơ</RouterLink>
      </div>
      <div v-if="message" class="alert alert-success py-2">{{ message }}</div>
      <div v-if="error" class="alert alert-danger py-2">{{ error }}</div>

      <div v-if="loading" class="text-center py-5 text-muted">Đang tải danh sách địa chỉ...</div>

      <div v-else-if="!addresses.length" class="card p-5 text-center text-muted">
        <p class="mb-3">Bạn chưa có địa chỉ nào.</p>
        <button type="button" class="btn btn-outline-primary" @click="openCreateForm">Thêm địa chỉ đầu tiên</button>
      </div>

      <div v-else class="row g-3">
        <div v-for="addr in addresses" :key="addr.id" class="col-md-6">
          <div class="card address-card h-100" :class="{ 'address-card--default': addr.isDefault }">
            <div class="card-body">
              <div class="d-flex justify-content-between align-items-start mb-2">
                <div>
                  <span v-if="addr.label" class="badge bg-light text-dark border me-1">{{ addr.label }}</span>
                  <span v-if="addr.isDefault" class="badge bg-primary">Mặc định</span>
                </div>
              </div>
              <p class="mb-0">
                {{ addr.addressDetail }}, {{ addr.ward }}, {{ addr.province }}
              </p>
            </div>
            <div class="card-footer bg-white d-flex gap-2 flex-wrap">
              <button type="button" class="btn btn-sm btn-outline-secondary" @click="openEditForm(addr)">Sửa</button>
              <button
                v-if="!addr.isDefault"
                type="button"
                class="btn btn-sm btn-outline-primary"
                @click="setDefault(addr.id)"
              >
                Đặt mặc định
              </button>
              <button type="button" class="btn btn-sm btn-outline-danger ms-auto" @click="removeAddress(addr.id)">
                Xóa
              </button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="showForm" class="address-modal">
        <div class="address-modal__backdrop" @click="closeForm"></div>
        <div class="card address-modal__panel">
          <div class="card-header d-flex justify-content-between align-items-center">
            <strong>{{ editingId ? "Cập nhật địa chỉ" : "Thêm địa chỉ mới" }}</strong>
            <button type="button" class="btn-close" aria-label="Đóng" @click="closeForm"></button>
          </div>
          <form class="card-body" @submit.prevent="submitForm">
            <AddressFormFields v-model="form" :form-errors="formErrors" />

            <div class="d-flex justify-content-end gap-2 mt-3">
              <button type="button" class="btn btn-light" @click="closeForm">Hủy</button>
              <button type="submit" class="btn btn-primary" :disabled="saving">
                {{ saving ? "Đang lưu..." : "Lưu địa chỉ" }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { RouterLink } from "vue-router";
import MainLayout from "../layouts/MainLayout.vue";
import AddressFormFields from "../components/AddressFormFields.vue";
import {
  createAddressApi,
  deleteAddressApi,
  fetchAddressesApi,
  setDefaultAddressApi,
  updateAddressApi,
} from "../services/api";

const addresses = ref([]);
const accountFullName = ref("");
const accountPhone = ref("");
const loading = ref(true);
const saving = ref(false);
const message = ref("");
const error = ref("");
const showForm = ref(false);
const editingId = ref(null);
const formErrors = ref({});

const emptyForm = () => ({
  label: "",
  province: "",
  ward: "",
  addressDetail: "",
  isDefault: false,
});

const form = reactive(emptyForm());

const loadAddresses = async () => {
  loading.value = true;
  error.value = "";
  try {
    const data = await fetchAddressesApi();
    addresses.value = data?.addresses || [];
    accountFullName.value = data?.accountFullName || "";
    accountPhone.value = data?.accountPhone || "";
  } catch (err) {
    error.value = err?.response?.data?.message || "Không thể tải danh sách địa chỉ.";
    addresses.value = [];
  } finally {
    loading.value = false;
  }
};

const resetForm = () => {
  Object.assign(form, emptyForm());
  formErrors.value = {};
};

const openCreateForm = () => {
  editingId.value = null;
  resetForm();
  showForm.value = true;
};

const openEditForm = (addr) => {
  editingId.value = addr.id;
  Object.assign(form, {
    label: addr.isDefault ? "" : addr.label || "",
    province: addr.province || "",
    ward: addr.ward || "",
    addressDetail: addr.addressDetail || "",
    isDefault: Boolean(addr.isDefault),
  });
  formErrors.value = {};
  showForm.value = true;
};

const closeForm = () => {
  showForm.value = false;
  editingId.value = null;
  resetForm();
};

const validateForm = () => {
  const errors = {};
  if (!form.province.trim()) errors.province = "Vui lòng nhập thành phố";
  if (!form.ward.trim()) errors.ward = "Vui lòng nhập phường";
  if (!form.addressDetail.trim()) errors.addressDetail = "Vui lòng nhập địa chỉ";
  formErrors.value = errors;
  return Object.keys(errors).length === 0;
};

const buildPayload = () => ({
  label: form.isDefault ? null : form.label.trim() || null,
  province: form.province.trim(),
  ward: form.ward.trim(),
  addressDetail: form.addressDetail.trim(),
  isDefault: form.isDefault,
});

const submitForm = async () => {
  if (!validateForm()) return;

  saving.value = true;
  message.value = "";
  error.value = "";
  try {
    const payload = buildPayload();
    if (editingId.value) {
      await updateAddressApi(editingId.value, payload);
      message.value = "Đã cập nhật địa chỉ.";
    } else {
      await createAddressApi(payload);
      message.value = "Đã thêm địa chỉ mới.";
    }
    closeForm();
    await loadAddresses();
  } catch (err) {
    error.value = err?.response?.data?.message || "Không thể lưu địa chỉ.";
  } finally {
    saving.value = false;
  }
};

const setDefault = async (id) => {
  error.value = "";
  try {
    await setDefaultAddressApi(id);
    message.value = "Đã đặt làm địa chỉ mặc định.";
    await loadAddresses();
  } catch (err) {
    error.value = err?.response?.data?.message || "Không thể đặt địa chỉ mặc định.";
  }
};

const removeAddress = async (id) => {
  if (!window.confirm("Bạn có chắc muốn xóa địa chỉ này?")) return;
  error.value = "";
  try {
    await deleteAddressApi(id);
    message.value = "Đã xóa địa chỉ.";
    await loadAddresses();
  } catch (err) {
    error.value = err?.response?.data?.message || "Không thể xóa địa chỉ.";
  }
};

onMounted(loadAddresses);
</script>

<style scoped>
.address-book__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
}

.address-card--default {
  border-color: #0d6efd;
  box-shadow: 0 0 0 1px rgba(13, 110, 253, 0.15);
}

.address-modal {
  position: fixed;
  inset: 0;
  z-index: 1050;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.address-modal__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
}

.address-modal__panel {
  position: relative;
  width: min(560px, 100%);
  max-height: 90vh;
  overflow-y: auto;
  z-index: 1;
}
</style>
