<template>
  <MainLayout>
    <div v-if="proceeding" class="checkout-page">
      <div class="checkout-page__head">
        <h1>Xác nhận đơn hàng</h1>
        <p>Hoàn tất thông tin giao hàng và xác nhận thanh toán</p>
      </div>

      <CheckoutSteps :current="tab === 'info' ? 'shipping' : 'confirm'" />

      <div v-if="error" class="checkout-alert checkout-alert--error">{{ error }}</div>

      <div class="checkout-layout">
        <div class="checkout-main">
          <div class="checkout-tabs">
            <button
              type="button"
              class="checkout-tabs__btn"
              :class="{ 'checkout-tabs__btn--active': tab === 'info' }"
              @click="tab = 'info'"
            >
              📍 Địa chỉ & Vận chuyển
            </button>
            <button
              type="button"
              class="checkout-tabs__btn"
              :class="{ 'checkout-tabs__btn--active': tab === 'payment' }"
              @click="goToPaymentTab"
            >
              ✅ Xác nhận
            </button>
          </div>

          <!-- Tab: Shipping -->
          <div v-if="tab === 'info'" class="checkout-card">
            <div class="checkout-card__head">
              <span class="checkout-card__head-icon">📍</span>
              Địa chỉ giao hàng
            </div>
            <div class="checkout-card__body">
              <div class="checkout-address-mode">
                <button
                  type="button"
                  class="checkout-address-mode__btn"
                  :class="{ 'checkout-address-mode__btn--active': addressMode === 'saved' }"
                  :disabled="!savedAddresses.length"
                  @click="addressMode = 'saved'"
                >
                  Địa chỉ đã lưu
                </button>
                <button
                  type="button"
                  class="checkout-address-mode__btn"
                  :class="{ 'checkout-address-mode__btn--active': addressMode === 'new' }"
                  @click="addressMode = 'new'"
                >
                  Nhập địa chỉ mới
                </button>
              </div>

              <div v-if="!savedAddresses.length && !addressesLoading" class="checkout-summary__hint mb-3">
                Bạn chưa có địa chỉ lưu.
                <RouterLink to="/addresses">Thêm vào sổ địa chỉ</RouterLink>
              </div>

              <div v-if="addressMode === 'saved'">
                <div v-if="addressesLoading" class="text-muted py-3">Đang tải địa chỉ...</div>
                <div v-else>
                  <div class="checkout-review mb-3">
                    <h6>Người nhận (từ tài khoản)</h6>
                    <p>
                      <strong>{{ accountFullName || "—" }}</strong>
                      <span v-if="accountPhone"> · {{ accountPhone }}</span>
                    </p>
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
                          <div class="small">{{ addr.addressDetail }}, {{ addr.ward }}, {{ addr.province }}</div>
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
            </div>
          </div>

          <div v-if="tab === 'info'" class="checkout-card">
            <div class="checkout-card__head">
              <span class="checkout-card__head-icon">🚚</span>
              Đơn vị vận chuyển
            </div>
            <div class="checkout-card__body">
              <div v-if="!carriers.length" class="text-muted small">Đang tải đơn vị vận chuyển...</div>
              <div v-else class="carrier-grid">
                <label
                  v-for="c in carriers"
                  :key="c.id"
                  class="carrier-card"
                  :class="{ 'carrier-card--active': selectedCarrierId === c.id }"
                >
                  <input v-model="selectedCarrierId" type="radio" :value="c.id" />
                  <div class="carrier-card__logo">📦</div>
                  <div class="carrier-card__name">{{ c.name }}</div>
                </label>
              </div>
              <div v-if="formErrors.carrier" class="text-danger small mt-2">{{ formErrors.carrier }}</div>
            </div>
          </div>

          <div v-if="tab === 'info'" class="d-flex justify-content-end">
            <button type="button" class="checkout-btn checkout-btn--primary" style="width: auto; min-width: 220px" @click="goToPaymentTab">
              Tiếp tục xác nhận →
            </button>
          </div>

          <!-- Tab: Confirm -->
          <div v-if="tab === 'payment'" class="checkout-card">
            <div class="checkout-card__head">
              <span class="checkout-card__head-icon">✅</span>
              Xác nhận & Thanh toán
            </div>
            <div class="checkout-card__body">
              <div class="checkout-review">
                <h6>Địa chỉ giao hàng</h6>
                <template v-if="resolvedShipping">
                  <p>
                    <strong>{{ resolvedShipping.receiverName }}</strong> · {{ resolvedShipping.receiverPhone }}<br />
                    {{ resolvedShipping.addressDetail }}, {{ resolvedShipping.ward }}, {{ resolvedShipping.province }}
                  </p>
                </template>
              </div>

              <div v-if="selectedCarrierName" class="checkout-review">
                <h6>Vận chuyển</h6>
                <p>{{ selectedCarrierName }} · Phí ship: <strong>{{ formatPrice(displayShippingFee) }}đ</strong></p>
              </div>

              <div class="checkout-review">
                <h6>Phương thức thanh toán</h6>
                <p>{{ paymentMethodLabel }}</p>
              </div>

              <button
                type="button"
                class="checkout-btn checkout-btn--success mt-3"
                :disabled="loading"
                @click="confirmPayment"
              >
                {{ loading ? "Đang xử lý..." : confirmButtonLabel }}
              </button>
              <button type="button" class="checkout-btn checkout-btn--outline mt-2" @click="tab = 'info'">
                ← Sửa thông tin giao hàng
              </button>
            </div>
          </div>
        </div>

        <aside class="checkout-summary">
          <div class="checkout-summary__head">
            <h2>Đơn hàng</h2>
          </div>
          <div class="checkout-summary__body">
            <div v-for="item in store.state.cartItems" :key="item.variantId || item.productId" class="checkout-summary__row">
              <span>{{ item.name }} ×{{ item.quantity }}</span>
              <span>{{ formatPrice(item.price * item.quantity) }}đ</span>
            </div>
            <div class="checkout-summary__row" style="margin-top: 12px">
              <span>Tạm tính</span>
              <span>{{ formatPrice(subTotal) }}đ</span>
            </div>
            <div class="checkout-summary__row">
              <span>Phí vận chuyển</span>
              <span>{{ displayShippingFee ? formatPrice(displayShippingFee) + "đ" : "Miễn phí" }}</span>
            </div>
            <div v-if="appliedVoucher" class="checkout-summary__row checkout-summary__row--discount">
              <span>Giảm giá ({{ appliedVoucher.code }})</span>
              <span>-{{ formatPrice(appliedVoucher.discountAmount) }}đ</span>
            </div>
            <div class="checkout-summary__total">
              <span>Tổng thanh toán</span>
              <strong>{{ formatPrice(checkoutTotal) }}đ</strong>
            </div>

            <div class="checkout-voucher no-print">
              <label class="checkout-voucher__label">Mã khuyến mãi</label>
              <div class="checkout-voucher__row">
                <input
                  v-model="voucherCode"
                  type="text"
                  class="form-control"
                  placeholder="Nhập mã (VD: SUMMER50K)"
                  :disabled="voucherLoading"
                />
                <button
                  type="button"
                  class="btn btn-outline-primary btn-sm"
                  :disabled="voucherLoading || !voucherCode.trim()"
                  @click="applyVoucher"
                >
                  {{ voucherLoading ? "..." : "Áp dụng" }}
                </button>
              </div>
              <div v-if="voucherError" class="checkout-voucher__error">{{ voucherError }}</div>
              <div v-if="appliedVoucher" class="checkout-voucher__ok">
                ✓ {{ appliedVoucher.name || appliedVoucher.code }} — giảm {{ formatPrice(appliedVoucher.discountAmount) }}đ
                <button type="button" class="btn btn-link btn-sm p-0 ms-1" @click="clearVoucher">Bỏ mã</button>
              </div>
            </div>
            <div class="checkout-summary__secure">
              🔒 Giao dịch được bảo mật
            </div>
          </div>
        </aside>
      </div>
    </div>

    <!-- Success -->
    <div v-else class="checkout-page">
      <div class="checkout-success">
        <div class="checkout-success__icon">✓</div>
        <h2>Đặt hàng thành công!</h2>
        <p class="text-muted mb-3">Cảm ơn bạn đã mua sắm tại Sports4Life</p>
        <div v-if="orderId" class="checkout-success__order">
          Mã đơn hàng: <strong>#{{ orderId }}</strong>
        </div>
        <RouterLink to="/product" class="checkout-btn checkout-btn--primary" style="max-width: 280px; margin: 0 auto">
          Tiếp tục mua sắm
        </RouterLink>
      </div>
    </div>
  </MainLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { RouterLink, useRoute } from "vue-router";
import MainLayout from "../../layouts/MainLayout.vue";
import AddressFormFields from "../../components/AddressFormFields.vue";
import CheckoutSteps from "../../components/CheckoutSteps.vue";
import { useAppStore } from "../../stores/appStore";
import { calcShippingFee } from "../../utils/shipping";
import { applyVoucherApi, confirmPaymentApi, fetchAddressesApi, fetchCheckoutCarriersApi } from "../../services/api";
import { submitSePayForm } from "../../utils/sepay";

const route = useRoute();
const store = useAppStore();

const subTotal = computed(() => store.cartAmount.value);
const baseShippingFee = computed(() => calcShippingFee(subTotal.value));
const displayShippingFee = computed(() => appliedVoucher.value?.shippingFee ?? baseShippingFee.value);
const checkoutTotal = computed(() => appliedVoucher.value?.totalAmount ?? (subTotal.value + baseShippingFee.value));

const voucherCode = ref("");
const appliedVoucher = ref(null);
const voucherError = ref("");
const voucherLoading = ref(false);

const tab = ref("info");
const proceeding = ref(true);
const success = ref(false);
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

const carriers = ref([]);
const selectedCarrierId = ref(null);

const shippingAddress = reactive({
  receiverName: "",
  receiverPhone: "",
  province: "",
  ward: "",
  addressDetail: "",
});

const paymentLabels = {
  CASH: "Thanh toán khi nhận hàng (COD)",
  SEPAY: "SePay — QR ngân hàng / Thẻ",
  MOMO: "Ví điện tử MoMo",
  TECHCOMBANK: "Chuyển khoản Techcombank",
};

const paymentMethodLabel = computed(() => paymentLabels[paymentMethod.value] || paymentMethod.value);
const confirmButtonLabel = computed(() => {
  if (paymentMethod.value === "SEPAY") return "Thanh toán qua SePay";
  return paymentMethod.value === "CASH" ? "Xác nhận đặt hàng" : "Tôi đã thanh toán";
});

const formatPrice = (price) => Number(price || 0).toLocaleString("vi-VN");

const selectedCarrierName = computed(() => {
  const carrier = carriers.value.find((c) => c.id === selectedCarrierId.value);
  return carrier?.name || "";
});

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
    }
  } else {
    if (!shippingAddress.receiverName.trim()) errors.receiverName = "Vui lòng nhập họ và tên người nhận";
    if (!shippingAddress.receiverPhone.trim()) errors.receiverPhone = "Vui lòng nhập số điện thoại người nhận";
    if (!shippingAddress.province.trim()) errors.province = "Vui lòng nhập thành phố";
    if (!shippingAddress.ward.trim()) errors.ward = "Vui lòng nhập phường";
    if (!shippingAddress.addressDetail.trim()) errors.addressDetail = "Vui lòng nhập địa chỉ";
  }

  if (!selectedCarrierId.value) {
    errors.carrier = "Vui lòng chọn đơn vị vận chuyển";
  }

  formErrors.value = errors;
  return Object.keys(errors).length === 0;
};

const goToPaymentTab = () => {
  error.value = "";
  if (!validateAddress()) {
    error.value = formErrors.value.general || formErrors.value.carrier || "Vui lòng điền đầy đủ thông tin giao hàng.";
    tab.value = "info";
    return;
  }
  tab.value = "payment";
};

const buildCheckoutPayload = () => ({
  paymentMethod: paymentMethod.value,
  amount: checkoutTotal.value,
  voucherCode: appliedVoucher.value?.voucherCode || voucherCode.value.trim() || null,
  addressMode: addressMode.value,
  carrierId: selectedCarrierId.value,
  items: store.state.cartItems.map((item) => ({
    productId: item.productId,
    variantId: item.variantId ?? null,
    quantity: item.quantity,
  })),
  ...(addressMode.value === "saved"
    ? { savedAddressId: selectedAddressId.value }
    : {
        shippingAddress: {
          receiverName: shippingAddress.receiverName.trim(),
          receiverPhone: shippingAddress.receiverPhone.trim(),
          province: shippingAddress.province.trim(),
          ward: shippingAddress.ward.trim(),
          addressDetail: shippingAddress.addressDetail.trim(),
        },
        saveToAddressBook: saveToAddressBook.value,
        setAsDefault: setAsDefault.value,
        addressBookLabel: setAsDefault.value ? null : newAddressLabel.value.trim() || null,
      }),
});

const buildVoucherItems = () => store.state.cartItems.map((item) => ({
  productId: item.productId,
  variantId: item.variantId ?? null,
  quantity: item.quantity,
}));

const applyVoucher = async () => {
  const code = voucherCode.value.trim();
  if (!code) return;
  if (!store.state.cartItems.length) {
    voucherError.value = "Giỏ hàng đang trống.";
    return;
  }
  voucherLoading.value = true;
  voucherError.value = "";
  try {
    const result = await applyVoucherApi({
      voucherCode: code,
      items: buildVoucherItems(),
    });
    appliedVoucher.value = {
      code: result.voucherCode,
      name: result.voucherName,
      discountAmount: Number(result.discountAmount || 0),
      shippingFee: Number(result.shippingFee ?? baseShippingFee.value),
      totalAmount: Number(result.totalAmount || 0),
      voucherCode: result.voucherCode,
    };
    voucherCode.value = result.voucherCode;
  } catch (err) {
    appliedVoucher.value = null;
    voucherError.value =
      err?.response?.data?.message ||
      err?.message ||
      "Mã khuyến mãi không hợp lệ.";
  } finally {
    voucherLoading.value = false;
  }
};

const clearVoucher = () => {
  appliedVoucher.value = null;
  voucherCode.value = "";
  voucherError.value = "";
};

onMounted(async () => {
  paymentMethod.value = String(route.query.method || "CASH");
  prefillFromProfile();
  await loadSavedAddresses();
  try {
    carriers.value = await fetchCheckoutCarriersApi();
  } catch (err) {
    console.warn("Không tải được danh sách vận chuyển", err);
  }
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

    if (paymentMethod.value === "SEPAY" && result?.sepay) {
      store.clearCart();
      submitSePayForm(result.sepay);
      return;
    }

    success.value = true;
    store.clearCart();
  } catch (err) {
    console.error("Xác nhận thanh toán thất bại", err);
    error.value =
      err?.response?.data?.message ||
      err?.message ||
      "Không thể hoàn tất đơn hàng. Vui lòng thử lại.";
    success.value = false;
  } finally {
    if (success.value) {
      proceeding.value = false;
    }
    loading.value = false;
  }
};
</script>
