<template>
  <MainLayout>
    <div class="container my-4" v-if="proceeding">
      <h2 class="mb-4">Xác nhận thanh toán</h2>
      <p>
        <strong>Tổng tiền:</strong>
        <span class="text-danger fw-bold">{{ formatPrice(amount) }} đ</span>
      </p>

      <ul class="nav nav-tabs mb-3">
        <li class="nav-item">
          <button class="nav-link" :class="{ active: tab === 'info' }" @click="tab = 'info'">
            Thông tin người nhận
          </button>
        </li>
        <li class="nav-item">
          <button class="nav-link" :class="{ active: tab === 'payment' }" @click="goToPaymentTab">
            Thanh toán
          </button>
        </li>
      </ul>

      <div v-if="error" class="alert alert-danger py-2">{{ error }}</div>

      <div v-if="tab === 'info'" class="card p-4">
        <h5 class="mb-3">Địa chỉ giao hàng</h5>

        <div class="mb-4">
          <div class="form-check form-check-inline">
            <input
              id="mode-saved"
              v-model="addressMode"
              class="form-check-input"
              type="radio"
              value="saved"
              :disabled="!savedAddresses.length"
            />
            <label class="form-check-label" for="mode-saved">Dùng địa chỉ đã lưu</label>
          </div>
          <div class="form-check form-check-inline">
            <input id="mode-new" v-model="addressMode" class="form-check-input" type="radio" value="new" />
            <label class="form-check-label" for="mode-new">Nhập địa chỉ mới</label>
          </div>
          <div v-if="!savedAddresses.length && !addressesLoading" class="form-text">
            Bạn chưa có địa chỉ lưu.
            <RouterLink to="/addresses">Thêm vào sổ địa chỉ</RouterLink>
          </div>
        </div>

        <div v-if="addressMode === 'saved'">
          <div v-if="addressesLoading" class="text-muted py-3">Đang tải địa chỉ...</div>
          <div v-else>
            <div class="alert alert-light border py-2 mb-3 small">
              <strong>Người nhận:</strong> {{ accountFullName || "—" }}
              <span v-if="accountPhone"> · {{ accountPhone }}</span>
              <span class="text-muted"> (lấy từ tài khoản)</span>
            </div>
            <div class="row g-3">
              <div v-for="addr in savedAddresses" :key="addr.id" class="col-md-6">
                <label class="address-option" :class="{ 'address-option--active': selectedAddressId === addr.id }">
                  <input v-model="selectedAddressId" type="radio" class="form-check-input me-2" :value="addr.id" />
                  <div>
                    <div class="d-flex gap-2 align-items-center mb-1">
                      <span v-if="addr.isDefault" class="badge bg-primary">Mặc định</span>
                      <span v-if="addr.label" class="badge bg-light text-dark border">{{ addr.label }}</span>
                    </div>
                    <div>{{ addr.addressDetail }}, {{ addr.ward }}, {{ addr.province }}</div>
                  </div>
                </label>
              </div>
            </div>
          </div>
        </div>

        <div v-else>
          <AddressFormFields
            v-model="shippingAddress"
            :form-errors="formErrors"
            :show-label="false"
            :show-default-checkbox="false"
            :show-receiver="true"
          />

          <div class="mt-3 border-top pt-3">
            <div class="form-check mb-2">
              <input id="save-to-book" v-model="saveToAddressBook" class="form-check-input" type="checkbox" />
              <label class="form-check-label" for="save-to-book">Lưu vào sổ địa chỉ</label>
            </div>
            <div v-if="saveToAddressBook" class="ms-4">
              <label class="form-label">Nhãn địa chỉ</label>
              <input v-model="newAddressLabel" type="text" class="form-control mb-2" placeholder="Nhà riêng, Công ty..." />
              <div class="form-check">
                <input id="set-default-checkout" v-model="setAsDefault" class="form-check-input" type="checkbox" />
                <label class="form-check-label" for="set-default-checkout">Đặt làm địa chỉ mặc định</label>
              </div>
            </div>
          </div>
        </div>

        <div class="mt-4 d-flex justify-content-end">
          <button type="button" class="btn btn-primary" @click="goToPaymentTab">Tiếp tục thanh toán</button>
        </div>
      </div>

      <div v-else class="card p-4">
        <h5 class="mb-3">Thanh toán</h5>

        <div class="shipping-summary mb-4 p-3 bg-light rounded">
          <h6 class="fw-bold mb-2">Địa chỉ giao hàng</h6>
          <template v-if="resolvedShipping">
            <p class="mb-1"><strong>{{ resolvedShipping.receiverName }}</strong> · {{ resolvedShipping.receiverPhone }}</p>
            <p class="mb-0 text-muted">
              {{ resolvedShipping.addressDetail }}, {{ resolvedShipping.ward }}, {{ resolvedShipping.province }}
            </p>
          </template>
        </div>

        <p v-if="paymentMethod === 'CASH'" class="mb-3">Bạn sẽ thanh toán khi nhận hàng.</p>
        <p v-if="paymentMethod === 'MOMO'" class="mb-3">Vui lòng xác nhận chuyển khoản qua MOMO.</p>
        <p v-if="paymentMethod === 'TECHCOMBANK'" class="mb-3">Vui lòng xác nhận chuyển khoản Techcombank.</p>
        <button class="btn btn-success" :disabled="loading" @click="confirmPayment">
          {{ loading ? "Đang xác nhận..." : "Tôi đã thanh toán" }}
        </button>
      </div>
    </div>

    <div v-else class="container">
      <div class="row justify-content-center align-items-center" style="min-height: 60vh">
        <div class="col-md-6">
          <div class="card shadow-sm text-center">
            <div class="card-body p-4">
              <h2 :class="success ? 'text-success' : 'text-danger'" class="fw-bold mb-3">
                {{ success ? "Đặt hàng thành công!" : "Có lỗi xảy ra" }}
              </h2>
              <p v-if="orderId" class="text-muted">Mã đơn hàng: #{{ orderId }}</p>
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
import { computed, onMounted, reactive, ref } from "vue";
import { RouterLink, useRoute } from "vue-router";
import MainLayout from "../../layouts/MainLayout.vue";
import AddressFormFields from "../../components/AddressFormFields.vue";
import { confirmPaymentApi, fetchAddressesApi } from "../../services/api";
import { useAppStore, useToast } from "../../stores/appStore";
import { firstError, getApiError, isValidPhone, runValidation } from "../../utils/validators";

const route = useRoute();
const store = useAppStore();
const toast = useToast();

const tab = ref("info");
const proceeding = ref(true);
const success = ref(false);
const amount = ref(0);
const paymentMethod = ref("CASH");
const loading = ref(false);
const error = ref("");
const orderId = ref(null);

const addressMode = ref("new");
const savedAddresses = ref([]);
const addressesLoading = ref(false);
const selectedAddressId = ref(null);
const saveToAddressBook = ref(false);
const setAsDefault = ref(false);
const newAddressLabel = ref("");
const formErrors = ref({});
const accountFullName = ref("");
const accountPhone = ref("");

const shippingAddress = reactive({
  receiverName: "",
  receiverPhone: "",
  province: "",
  ward: "",
  addressDetail: "",
});

const formatPrice = (price) => Number(price || 0).toLocaleString("vi-VN");

const resolvedShipping = computed(() => {
  if (addressMode.value === "saved") {
    const addr = savedAddresses.value.find((a) => a.id === selectedAddressId.value);
    if (!addr) return null;
    return {
      receiverName: accountFullName.value,
      receiverPhone: accountPhone.value,
      province: addr.province,
      ward: addr.ward,
      addressDetail: addr.addressDetail,
    };
  }
  return {
    receiverName: shippingAddress.receiverName,
    receiverPhone: shippingAddress.receiverPhone,
    province: shippingAddress.province,
    ward: shippingAddress.ward,
    addressDetail: shippingAddress.addressDetail,
  };
});

const loadSavedAddresses = async () => {
  if (!store.state.user) return;
  addressesLoading.value = true;
  try {
    const data = await fetchAddressesApi();
    savedAddresses.value = data?.addresses || [];
    accountFullName.value = data?.accountFullName || store.state.user?.fullname || store.state.user?.username || "";
    accountPhone.value = data?.accountPhone || store.state.user?.phone || "";
    const defaultAddr = savedAddresses.value.find((a) => a.isDefault);
    if (defaultAddr) {
      selectedAddressId.value = defaultAddr.id;
      addressMode.value = "saved";
    } else if (savedAddresses.value.length) {
      selectedAddressId.value = savedAddresses.value[0].id;
      addressMode.value = "saved";
    }
  } catch (err) {
    console.warn("Không thể tải địa chỉ đã lưu", err);
    savedAddresses.value = [];
  } finally {
    addressesLoading.value = false;
  }
};

const prefillFromProfile = () => {
  const user = store.state.user;
  shippingAddress.receiverName = user?.fullname || user?.username || "";
  shippingAddress.receiverPhone = user?.phone || "";
};

const validateAddress = () => {
  const errors = {};
  if (addressMode.value === "saved") {
    if (!selectedAddressId.value) errors.general = "Vui lòng chọn địa chỉ giao hàng";
    if (!accountFullName.value?.trim()) {
      errors.general = "Vui lòng cập nhật họ tên trong hồ sơ trước khi đặt hàng";
    }
    if (!accountPhone.value?.trim()) {
      errors.general = "Vui lòng cập nhật số điện thoại trong hồ sơ trước khi đặt hàng";
    } else if (!isValidPhone(accountPhone.value)) {
      errors.general = "Số điện thoại trong hồ sơ không hợp lệ. Vui lòng cập nhật lại.";
    }
    formErrors.value = errors;
    return !errors.general;
  }

  const result = runValidation(shippingAddress, {
    receiverName: [
      "required",
      { type: "min", min: 2, message: "Họ tên tối thiểu 2 ký tự." },
      { type: "max", max: 100 },
    ],
    receiverPhone: ["required", "phone"],
    province: ["required", { type: "max", max: 100 }],
    ward: ["required", { type: "max", max: 100 }],
    addressDetail: ["required", { type: "max", max: 255 }],
  });
  formErrors.value = result.errors;
  return result.ok;
};

const goToPaymentTab = () => {
  error.value = "";
  if (!validateAddress()) {
    error.value = formErrors.value.general || firstError(formErrors.value) || "Vui lòng điền đầy đủ thông tin giao hàng.";
    toast.error(error.value);
    tab.value = "info";
    return;
  }
  tab.value = "payment";
};

const buildCheckoutPayload = () => {
  const payload = {
    paymentMethod: paymentMethod.value,
    amount: amount.value,
    addressMode: addressMode.value,
    items: store.state.cartItems.map((item) => ({
      productId: item.productId,
      variantId: item.variantId ?? null,
      quantity: item.quantity,
    })),
  };

  if (addressMode.value === "saved") {
    payload.savedAddressId = selectedAddressId.value;
  } else {
    payload.shippingAddress = {
      receiverName: shippingAddress.receiverName.trim(),
      receiverPhone: shippingAddress.receiverPhone.trim(),
      province: shippingAddress.province.trim(),
      ward: shippingAddress.ward.trim(),
      addressDetail: shippingAddress.addressDetail.trim(),
    };
    payload.saveToAddressBook = saveToAddressBook.value;
    payload.setAsDefault = setAsDefault.value;
    payload.addressBookLabel = setAsDefault.value ? null : newAddressLabel.value.trim() || null;
  }

  return payload;
};

onMounted(async () => {
  amount.value = Number(route.query.amount || store.cartAmount.value || 0);
  paymentMethod.value = String(route.query.method || "CASH");
  prefillFromProfile();
  await loadSavedAddresses();
});

const confirmPayment = async () => {
  if (!store.state.cartItems.length) {
    error.value = "Giỏ hàng đang trống.";
    return;
  }

  if (!validateAddress()) {
    tab.value = "info";
    error.value = "Vui lòng hoàn thiện thông tin giao hàng trước khi thanh toán.";
    return;
  }

  loading.value = true;
  error.value = "";
  try {
    const result = await confirmPaymentApi(buildCheckoutPayload());
    orderId.value = result?.orderId || null;
    success.value = true;
    store.clearCart();
  } catch (err) {
    console.error("Xác nhận thanh toán thất bại", err);
    const api = getApiError(err, "Không thể hoàn tất đơn hàng. Vui lòng thử lại.");
    error.value = api.message;
    Object.assign(formErrors.value, api.errors);
    toast.error(error.value);
    success.value = false;
  } finally {
    if (success.value) {
      proceeding.value = false;
    }
    loading.value = false;
  }
};
</script>

<style scoped>
.address-option {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  width: 100%;
  padding: 1rem;
  border: 1px solid #dee2e6;
  border-radius: 0.5rem;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.address-option--active {
  border-color: #0d6efd;
  box-shadow: 0 0 0 1px rgba(13, 110, 253, 0.2);
  background: #f8fbff;
}

.shipping-summary {
  border-left: 4px solid #0d6efd;
}
</style>
