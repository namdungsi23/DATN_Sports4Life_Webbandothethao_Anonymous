<template>
  <MainLayout>
    <div class="profile-page">
      <div v-if="loading" class="profile-loading">Đang tải thông tin...</div>

      <template v-else>
        <section class="profile-hero">
          <div class="profile-hero__avatar">
            <img v-if="avatarPreview" :src="avatarPreview" :alt="displayName" />
            <span v-else class="profile-hero__initials">{{ initials }}</span>
            <label v-if="editing" class="profile-hero__avatar-edit" title="Đổi ảnh">
              <input type="file" accept="image/*" hidden @change="onAvatarPick" />
              📷
            </label>
          </div>
          <div class="profile-hero__info">
            <p class="profile-hero__eyebrow">Thành viên Sports4Life</p>
            <h1>{{ displayName }}</h1>
            <p class="profile-hero__username">@{{ profile.username || "user" }}</p>
            <p v-if="profile.rankName" class="profile-hero__rank">
              Hạng <strong>{{ profile.rankName }}</strong>
              <span v-if="profile.totalPoint != null"> · {{ profile.totalPoint }} điểm</span>
            </p>
          </div>
          <button type="button" class="profile-hero__logout" @click="onLogout">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M9 6V4a1 1 0 0 1 1-1h9a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1h-9a1 1 0 0 1-1-1v-2" stroke-linecap="round" />
              <path d="M14 12H3m0 0l3.5-3.5M3 12l3.5 3.5" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
            Đăng xuất
          </button>
        </section>

        <div v-if="message" class="profile-alert profile-alert--success">{{ message }}</div>
        <div v-if="error" class="profile-alert profile-alert--error">{{ error }}</div>

        <div class="profile-stats">
          <div class="profile-stat">
            <strong>{{ profile.rankName || "—" }}</strong>
            <span>Hạng thành viên</span>
          </div>
          <div class="profile-stat">
            <strong>{{ profile.totalPoint ?? 0 }}</strong>
            <span>Điểm tích lũy</span>
          </div>
          <div class="profile-stat">
            <strong>{{ cartCount }}</strong>
            <span>Sản phẩm trong giỏ</span>
          </div>
          <div class="profile-stat">
            <strong>{{ memberSince }}</strong>
            <span>Thành viên từ</span>
          </div>
        </div>

        <div class="profile-layout">
          <aside class="profile-sidebar">
            <nav class="profile-nav">
              <button
                v-for="tab in tabs"
                :key="tab.id"
                type="button"
                class="profile-nav__item"
                :class="{ 'is-active': activeTab === tab.id }"
                @click="activeTab = tab.id"
              >
                <span class="profile-nav__icon">{{ tab.icon }}</span>
                {{ tab.label }}
              </button>
            </nav>

            <div class="profile-sidebar__links">
              <RouterLink to="/favorites" class="profile-quick-link">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path
                    d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
                    stroke-linejoin="round"
                  />
                </svg>
                Yêu thích
                <span v-if="favoriteCount" class="profile-quick-link__badge">{{ favoriteCount }}</span>
              </RouterLink>
              <RouterLink to="/addresses" class="profile-quick-link">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M12 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6z" />
                  <path d="M12 22s8-4.5 8-11a8 8 0 1 0-16 0c0 6.5 8 11 8 11z" stroke-linejoin="round" />
                </svg>
                Sổ địa chỉ
              </RouterLink>
              <RouterLink to="/cart" class="profile-quick-link">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M6 6h15l-1.5 9h-12L6 6z" stroke-linejoin="round" />
                  <circle cx="9" cy="20" r="1.5" fill="currentColor" stroke="none" />
                  <circle cx="18" cy="20" r="1.5" fill="currentColor" stroke="none" />
                </svg>
                Giỏ hàng
                <span v-if="cartCount" class="profile-quick-link__badge">{{ cartCount }}</span>
              </RouterLink>
              <RouterLink v-if="canAccessPanel" to="/admin/dashboard" class="profile-quick-link profile-quick-link--admin">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <rect x="3" y="3" width="7" height="7" rx="1" />
                  <rect x="14" y="3" width="7" height="7" rx="1" />
                  <rect x="3" y="14" width="7" height="7" rx="1" />
                  <rect x="14" y="14" width="7" height="7" rx="1" />
                </svg>
                Quản trị
              </RouterLink>
            </div>
          </aside>

          <div class="profile-content">
            <section v-show="activeTab === 'account'" class="profile-panel">
              <div class="profile-panel__head">
                <div>
                  <h2>Thông tin tài khoản</h2>
                  <p class="profile-panel__desc">Cập nhật họ tên, email, số điện thoại và ảnh đại diện.</p>
                </div>
                <button
                  v-if="!editing"
                  type="button"
                  class="profile-edit-btn"
                  @click="startEdit"
                >
                  Chỉnh sửa
                </button>
              </div>

              <form v-if="editing" class="profile-form" @submit.prevent="saveProfile">
                <div class="profile-form__avatar">
                  <img v-if="avatarPreview" :src="avatarPreview" alt="Ảnh đại diện" />
                  <span v-else class="profile-hero__initials">{{ initials }}</span>
                  <label class="profile-form__upload">
                    <input type="file" accept="image/*" @change="onAvatarPick" />
                    Tải ảnh lên Cloudinary
                  </label>
                  <input
                    v-model="form.photo"
                    type="url"
                    class="profile-form__input"
                    placeholder="Hoặc dán URL — sẽ đồng bộ Cloudinary khi lưu"
                  />
                  <small class="profile-form__hint">Ảnh lưu Cloudinary + SQL (Users.Avatar)</small>
                </div>

                <div class="profile-form__grid">
                  <label class="profile-form__field">
                    <span>Tên đăng nhập</span>
                    <input :value="profile.username" type="text" disabled />
                  </label>
                  <label class="profile-form__field">
                    <span>Họ và tên</span>
                    <input v-model="form.fullname" type="text" placeholder="Họ và tên" />
                  </label>
                  <label class="profile-form__field">
                    <span>Email</span>
                    <input v-model="form.email" type="email" placeholder="Email" />
                  </label>
                  <label class="profile-form__field">
                    <span>Số điện thoại</span>
                    <input v-model="form.phone" type="tel" placeholder="Số điện thoại" />
                  </label>
                </div>

                <div class="profile-form__actions">
                  <button type="button" class="profile-form__cancel" @click="cancelEdit">Hủy</button>
                  <button type="submit" class="profile-form__save" :disabled="saving">
                    {{ saving ? "Đang lưu..." : "Lưu thay đổi" }}
                  </button>
                </div>
              </form>

              <div v-else class="profile-fields">
                <div class="profile-field">
                  <label>Tên đăng nhập</label>
                  <div class="profile-field__value">{{ profile.username || "—" }}</div>
                </div>
                <div class="profile-field">
                  <label>Họ và tên</label>
                  <div class="profile-field__value">{{ profile.fullname || "—" }}</div>
                </div>
                <div class="profile-field">
                  <label>Email</label>
                  <div class="profile-field__value">{{ profile.email || "—" }}</div>
                </div>
                <div class="profile-field">
                  <label>Số điện thoại</label>
                  <div class="profile-field__value">{{ profile.phone || "Chưa cập nhật" }}</div>
                </div>
                <div class="profile-field">
                  <label>Hạng thành viên</label>
                  <div class="profile-field__value">
                    {{ profile.rankName || "—" }}
                    <span v-if="profile.rankDiscountPercent" class="text-muted">
                      (giảm {{ profile.rankDiscountPercent }}%)
                    </span>
                  </div>
                </div>
                <div class="profile-field">
                  <label>Điểm tích lũy</label>
                  <div class="profile-field__value">{{ profile.totalPoint ?? 0 }}</div>
                </div>
              </div>
            </section>

            <section v-show="activeTab === 'orders'" class="profile-panel">
              <h2>Đơn hàng của tôi</h2>
              <p class="profile-panel__desc">Theo dõi trạng thái và chi tiết các đơn đã đặt.</p>

              <div v-if="ordersLoading" class="profile-loading">Đang tải đơn hàng...</div>

              <div v-else-if="!myOrders.length" class="profile-empty">
                <p>Bạn chưa có đơn hàng nào.</p>
                <RouterLink to="/product" class="profile-empty__btn">Mua sắm ngay</RouterLink>
              </div>

              <div v-else class="profile-orders">
                <article
                  v-for="order in myOrders"
                  :key="order.id"
                  class="profile-order-card"
                  :class="{ 'profile-order-card--open': expandedOrderId === order.id }"
                >
                  <div class="profile-order-card__head">
                    <div>
                      <strong>Đơn #{{ order.id }}</strong>
                      <p class="profile-order-card__date">{{ formatOrderDate(order.createDate) }}</p>
                    </div>
                    <div class="profile-order-card__badges">
                      <span class="profile-order-badge" :class="orderStatusClass(order.orderStatus)">
                        {{ orderStatusLabel(order.orderStatus) }}
                      </span>
                      <span class="profile-order-badge profile-order-badge--muted">
                        {{ paymentStatusLabel(order.paymentStatus) }}
                      </span>
                    </div>
                  </div>

                  <div class="profile-order-card__meta">
                    <span>{{ order.itemCount || 0 }} sản phẩm</span>
                    <strong>{{ formatPrice(order.totalAmount) }}đ</strong>
                  </div>
                  <p v-if="order.address" class="profile-order-card__addr">{{ order.address }}</p>

                  <button
                    type="button"
                    class="profile-order-card__toggle"
                    @click="toggleOrderDetail(order.id)"
                  >
                    {{ expandedOrderId === order.id ? "Thu gọn" : "Xem chi tiết" }}
                  </button>

                  <div v-if="expandedOrderId === order.id" class="profile-order-detail">
                    <div v-if="orderDetailLoading" class="text-muted small py-2">Đang tải chi tiết...</div>
                    <template v-else-if="orderDetail">
                      <ul v-if="orderDetail.items?.length" class="profile-order-items">
                        <li v-for="item in orderDetail.items" :key="item.id">
                          <div>
                            <strong>{{ item.productName || "Sản phẩm" }}</strong>
                            <p v-if="item.size || item.color" class="profile-order-item__variant">
                              {{ [item.size, item.color].filter(Boolean).join(" · ") }}
                            </p>
                          </div>
                          <div class="profile-order-item__qty">
                            {{ item.quantity }} × {{ formatPrice(item.price) }}đ
                          </div>
                          <strong>{{ formatPrice(item.lineTotal) }}đ</strong>
                        </li>
                      </ul>

                      <div v-if="orderDetail.shippingAddress" class="profile-order-shipping">
                        <h4>Địa chỉ giao hàng</h4>
                        <p>
                          <strong>{{ orderDetail.shippingAddress.receiverName }}</strong>
                          · {{ orderDetail.shippingAddress.receiverPhone }}
                        </p>
                        <p class="text-muted">
                          {{ orderDetail.shippingAddress.addressDetail }},
                          {{ orderDetail.shippingAddress.ward }},
                          {{ orderDetail.shippingAddress.province }}
                        </p>
                      </div>

                      <div v-if="orderDetail.shipment" class="profile-order-shipment">
                        <h4>Vận chuyển</h4>
                        <p v-if="orderDetail.shipment.carrierName">
                          Đơn vị: <strong>{{ orderDetail.shipment.carrierName }}</strong>
                        </p>
                        <p v-if="orderDetail.shipment.trackingNumber">
                          Mã vận đơn: <strong>{{ orderDetail.shipment.trackingNumber }}</strong>
                        </p>
                        <p>
                          Trạng thái:
                          <strong>{{ shippingStatusLabel(orderDetail.shipment.shippingStatus) }}</strong>
                        </p>
                      </div>
                    </template>
                  </div>
                </article>
              </div>

              <RouterLink to="/cart" class="profile-panel__action">Xem giỏ hàng →</RouterLink>
            </section>

            <section v-show="activeTab === 'security'" class="profile-panel">
              <h2>Bảo mật</h2>
              <p class="profile-panel__desc">Đổi mật khẩu đăng nhập và giữ tài khoản an toàn.</p>

              <div class="profile-fields">
                <div class="profile-field">
                  <label>Trạng thái đăng nhập</label>
                  <div class="profile-field__value profile-field__value--success">● Đang hoạt động</div>
                </div>
                <div class="profile-field">
                  <label>Phương thức</label>
                  <div class="profile-field__value">Tài khoản Sports4Life</div>
                </div>
              </div>

              <form class="profile-form profile-form--password" @submit.prevent="submitChangePassword">
                <h3 class="profile-form__title">Đổi mật khẩu</h3>
                <div v-if="pwdError" class="profile-alert profile-alert--error">{{ pwdError }}</div>
                <div v-if="pwdSuccess" class="profile-alert profile-alert--success">{{ pwdSuccess }}</div>

                <div class="profile-form__grid">
                  <label class="profile-form__field">
                    <span>Mật khẩu hiện tại *</span>
                    <input
                      v-model="pwdForm.currentPassword"
                      type="password"
                      autocomplete="current-password"
                      placeholder="Mật khẩu hiện tại"
                      :class="{ 'is-invalid': pwdFieldErrors.currentPassword }"
                      @input="clearPwdError('currentPassword')"
                    />
                    <small v-if="pwdFieldErrors.currentPassword" class="profile-form__error">{{
                      pwdFieldErrors.currentPassword
                    }}</small>
                  </label>
                  <label class="profile-form__field">
                    <span>Mật khẩu mới *</span>
                    <input
                      v-model="pwdForm.newPassword"
                      type="password"
                      autocomplete="new-password"
                      placeholder="Mật khẩu mới"
                      :class="{ 'is-invalid': pwdFieldErrors.newPassword }"
                      @input="clearPwdError('newPassword')"
                    />
                    <small v-if="pwdFieldErrors.newPassword" class="profile-form__error">{{
                      pwdFieldErrors.newPassword
                    }}</small>
                  </label>
                  <label class="profile-form__field">
                    <span>Nhập lại mật khẩu mới *</span>
                    <input
                      v-model="pwdForm.confirmPassword"
                      type="password"
                      autocomplete="new-password"
                      placeholder="Nhập lại mật khẩu"
                      :class="{ 'is-invalid': pwdFieldErrors.confirmPassword }"
                      @input="clearPwdError('confirmPassword')"
                    />
                    <small v-if="pwdFieldErrors.confirmPassword" class="profile-form__error">{{
                      pwdFieldErrors.confirmPassword
                    }}</small>
                  </label>
                </div>

                <div class="profile-form__actions">
                  <button type="submit" class="profile-form__save" :disabled="pwdSaving">
                    {{ pwdSaving ? "Đang lưu..." : "Cập nhật mật khẩu" }}
                  </button>
                </div>
              </form>
            </section>
          </div>
        </div>
      </template>
    </div>
  </MainLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import MainLayout from "../layouts/MainLayout.vue";
import {
  changePasswordApi,
  fetchMyOrderDetailApi,
  fetchMyOrdersApi,
  fetchProfileApi,
  updateProfileApi,
  uploadProfileAvatarApi,
} from "../services/api";
import { useAppStore, useToast } from "../stores/appStore";
import { firstError, getApiError, normalizePhone, runValidation } from "../utils/validators";
import {
  ORDER_STATUS_LABELS,
  PAYMENT_STATUS_LABELS,
  SHIPPING_STATUS_LABELS,
  userCanAccessPanel,
} from "../utils/adminAccess";

const route = useRoute();
const router = useRouter();
const store = useAppStore();
const toast = useToast();
const loading = ref(true);
const saving = ref(false);
const editing = ref(false);
const activeTab = ref("account");
const profile = ref({});
const message = ref("");
const error = ref("");
const avatarFile = ref(null);
const myOrders = ref([]);
const ordersLoading = ref(false);
const expandedOrderId = ref(null);
const orderDetail = ref(null);
const orderDetailLoading = ref(false);

const form = reactive({
  fullname: "",
  email: "",
  phone: "",
  photo: "",
});

const pwdForm = reactive({
  currentPassword: "",
  newPassword: "",
  confirmPassword: "",
});
const pwdFieldErrors = reactive({});
const pwdSaving = ref(false);
const pwdError = ref("");
const pwdSuccess = ref("");

const clearPwdError = (key) => {
  delete pwdFieldErrors[key];
  if (pwdError.value) pwdError.value = "";
};

const submitChangePassword = async () => {
  Object.keys(pwdFieldErrors).forEach((k) => delete pwdFieldErrors[k]);
  pwdError.value = "";
  pwdSuccess.value = "";

  const result = runValidation(pwdForm, {
    currentPassword: [{ type: "required", message: "Vui lòng nhập mật khẩu hiện tại." }],
    newPassword: ["required", "password"],
    confirmPassword: [
      "required",
      { type: "match", field: "newPassword", message: "Mật khẩu nhập lại không khớp." },
    ],
  });

  if (!result.ok) {
    Object.assign(pwdFieldErrors, result.errors);
    pwdError.value = firstError(result.errors);
    toast.error(pwdError.value);
    return;
  }

  pwdSaving.value = true;
  try {
    const data = await changePasswordApi({
      currentPassword: pwdForm.currentPassword,
      newPassword: pwdForm.newPassword,
      confirmPassword: pwdForm.confirmPassword,
    });
    pwdSuccess.value = data?.message || "Đổi mật khẩu thành công.";
    toast.success(pwdSuccess.value);
    pwdForm.currentPassword = "";
    pwdForm.newPassword = "";
    pwdForm.confirmPassword = "";
  } catch (err) {
    const api = getApiError(err, "Đổi mật khẩu thất bại. Vui lòng thử lại.");
    Object.assign(pwdFieldErrors, api.errors || {});
    pwdError.value = api.message;
    toast.error(pwdError.value);
  } finally {
    pwdSaving.value = false;
  }
};

const tabs = [
  { id: "account", label: "Tài khoản", icon: "👤" },
  { id: "orders", label: "Đơn hàng", icon: "📦" },
  { id: "security", label: "Bảo mật", icon: "🔒" },
];

const cartCount = computed(() => store.cartCount.value);
const cartAmount = computed(() => store.cartAmount.value);
const favoriteCount = computed(() => store.favoriteCount.value);
const canAccessPanel = computed(() => userCanAccessPanel(store.state.user));

const displayName = computed(
  () => profile.value.fullname || profile.value.username || "Thành viên"
);

const avatarPreview = computed(
  () => form.photo || profile.value.photo || profile.value.avatar || ""
);

const initials = computed(() => {
  const name = displayName.value.trim();
  const parts = name.split(/\s+/);
  if (parts.length >= 2) return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  return name.slice(0, 2).toUpperCase();
});

const memberSince = computed(() => {
  const d = profile.value.createDate || profile.value.createdAt;
  if (d) return new Date(d).getFullYear();
  return new Date().getFullYear();
});

const formatPrice = (price) => Number(price || 0).toLocaleString("vi-VN");
const formatOrderDate = (value) => {
  if (!value) return "—";
  return new Date(value).toLocaleString("vi-VN");
};
const orderStatusLabel = (status) => ORDER_STATUS_LABELS[status] || status || "—";
const paymentStatusLabel = (status) => PAYMENT_STATUS_LABELS[status] || status || "—";
const shippingStatusLabel = (status) => SHIPPING_STATUS_LABELS[status] || status || "—";
const orderStatusClass = (status) => {
  const map = {
    PENDING: "profile-order-badge--pending",
    CONFIRMED: "profile-order-badge--confirmed",
    SHIPPING: "profile-order-badge--shipping",
    DELIVERED: "profile-order-badge--delivered",
    CANCELLED: "profile-order-badge--cancelled",
  };
  return map[status] || "profile-order-badge--muted";
};

const loadMyOrders = async () => {
  ordersLoading.value = true;
  try {
    const data = await fetchMyOrdersApi();
    myOrders.value = Array.isArray(data) ? data : data?.orders || [];
  } catch (err) {
    toast.error(err?.response?.data?.message || err?.message || "Không thể tải đơn hàng.");
    myOrders.value = [];
  } finally {
    ordersLoading.value = false;
  }
};

const toggleOrderDetail = async (orderId) => {
  if (expandedOrderId.value === orderId) {
    expandedOrderId.value = null;
    orderDetail.value = null;
    return;
  }
  expandedOrderId.value = orderId;
  orderDetail.value = null;
  orderDetailLoading.value = true;
  try {
    orderDetail.value = await fetchMyOrderDetailApi(orderId);
  } catch (err) {
    toast.error(err?.response?.data?.message || "Không thể tải chi tiết đơn hàng.");
    expandedOrderId.value = null;
  } finally {
    orderDetailLoading.value = false;
  }
};

const applyProfile = (data) => {
  profile.value = { ...(store.state.user || {}), ...(data?.profile || data || {}) };
  form.fullname = profile.value.fullname || profile.value.fullName || "";
  form.email = profile.value.email || "";
  form.phone = profile.value.phone || "";
  form.photo = profile.value.photo || profile.value.avatar || "";
};

const syncStore = () => {
  store.updateUserProfile({
    fullname: profile.value.fullname,
    fullName: profile.value.fullname,
    email: profile.value.email,
    phone: profile.value.phone,
    photo: profile.value.photo || profile.value.avatar,
    avatar: profile.value.photo || profile.value.avatar,
  });
};

const startEdit = () => {
  editing.value = true;
  message.value = "";
  error.value = "";
  avatarFile.value = null;
};

const cancelEdit = () => {
  editing.value = false;
  avatarFile.value = null;
  form.fullname = profile.value.fullname || "";
  form.email = profile.value.email || "";
  form.phone = profile.value.phone || "";
  form.photo = profile.value.photo || profile.value.avatar || "";
};

const onAvatarPick = (event) => {
  const file = event.target.files?.[0];
  if (!file) return;
  avatarFile.value = file;
  form.photo = URL.createObjectURL(file);
};

const saveProfile = async () => {
  const result = runValidation(
    {
      fullname: form.fullname,
      email: form.email,
      phone: form.phone,
      photo: avatarFile.value || (form.photo?.startsWith("blob:") ? null : form.photo),
    },
    {
      fullname: [
        "required",
        { type: "min", min: 2, message: "Họ tên tối thiểu 2 ký tự." },
        { type: "max", max: 100 },
      ],
      email: ["required", "email", { type: "max", max: 100 }],
      phone: ["required", "phone"],
      photo: ["fileImage"],
    }
  );

  if (!result.ok) {
    error.value = firstError(result.errors);
    toast.error(error.value);
    return;
  }

  if (form.photo && !form.photo.startsWith("blob:") && !form.photo.startsWith("http")) {
    error.value = "Link ảnh phải bắt đầu bằng http:// hoặc https://";
    toast.error(error.value);
    return;
  }

  saving.value = true;
  message.value = "";
  error.value = "";

  try {
    let data;

    if (avatarFile.value) {
      data = await uploadProfileAvatarApi(avatarFile.value);
      applyProfile(data);
      avatarFile.value = null;
    }

    const payload = {
      fullname: result.values.fullname,
      email: result.values.email,
      phone: normalizePhone(form.phone),
    };

    if (form.photo && !form.photo.startsWith("blob:")) {
      payload.photo = form.photo.trim();
    }

    data = await updateProfileApi(payload);
    applyProfile(data);
    syncStore();
    editing.value = false;
    message.value = data?.message || "Cập nhật hồ sơ thành công.";
    toast.success(message.value);
  } catch (err) {
    console.warn("Update profile failed", err);
    const api = getApiError(err, "Không thể cập nhật hồ sơ. Vui lòng thử lại.");
    error.value = api.message;
    toast.error(error.value);
  } finally {
    saving.value = false;
  }
};

const onLogout = () => {
  store.logout();
  router.push("/login");
};

onMounted(async () => {
  if (!store.state.user) {
    router.push("/login?redirect=/profile");
    return;
  }

  if (route.query.tab === "orders") {
    activeTab.value = "orders";
  }

  try {
    const data = await fetchProfileApi();
    applyProfile(data);
    syncStore();
    await store.loadFavorites();
  } catch {
    profile.value = store.state.user || {};
  } finally {
    loading.value = false;
  }

  if (activeTab.value === "orders") {
    loadMyOrders();
  }
});

watch(activeTab, (tab) => {
  if (tab === "orders") {
    loadMyOrders();
  }
});
</script>
