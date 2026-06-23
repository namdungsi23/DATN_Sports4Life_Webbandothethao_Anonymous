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
            <div class="profile-hero__tags">
              <span v-for="role in roleLabels" :key="role" class="profile-tag">{{ role }}</span>
            </div>
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
            <strong>{{ cartCount }}</strong>
            <span>Sản phẩm trong giỏ</span>
          </div>
          <div class="profile-stat">
            <strong>{{ formatPrice(cartAmount) }}đ</strong>
            <span>Giá trị giỏ hàng</span>
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
                    Chọn ảnh từ máy
                  </label>
                  <input
                    v-model="form.photo"
                    type="url"
                    class="profile-form__input"
                    placeholder="Hoặc dán link ảnh (https://...)"
                  />
                </div>

                <div class="profile-form__grid">
                  <label class="profile-form__field">
                    <span>Tên đăng nhập</span>
                    <input :value="profile.username" type="text" disabled />
                  </label>
                  <label class="profile-form__field">
                    <span>Họ và tên</span>
                    <input v-model="form.fullname" type="text" placeholder="Nguyễn Văn A" />
                  </label>
                  <label class="profile-form__field">
                    <span>Email</span>
                    <input v-model="form.email" type="email" placeholder="email@example.com" />
                  </label>
                  <label class="profile-form__field">
                    <span>Số điện thoại</span>
                    <input v-model="form.phone" type="tel" placeholder="09xxxxxxxx" />
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
                  <label>Vai trò</label>
                  <div class="profile-field__value">{{ roleLabels.join(", ") || "Khách hàng" }}</div>
                </div>
              </div>
            </section>

            <section v-show="activeTab === 'orders'" class="profile-panel">
              <h2>Giỏ hàng của bạn</h2>
              <p class="profile-panel__desc">Xem nhanh sản phẩm đang có trong giỏ.</p>

              <div v-if="!cartItems.length" class="profile-empty">
                <p>Giỏ hàng trống.</p>
                <RouterLink to="/product" class="profile-empty__btn">Mua sắm ngay</RouterLink>
              </div>

              <ul v-else class="profile-cart-list">
                <li v-for="item in cartItems" :key="item.productId" class="profile-cart-item">
                  <ProductImage :src="item.image" :alt="item.name" />
                  <div>
                    <h4>{{ item.name }}</h4>
                    <p>{{ item.quantity }} × {{ formatPrice(item.price) }}đ</p>
                  </div>
                  <strong>{{ formatPrice(item.price * item.quantity) }}đ</strong>
                </li>
              </ul>

              <RouterLink v-if="cartItems.length" to="/cart" class="profile-panel__action">
                Xem giỏ hàng & thanh toán →
              </RouterLink>
            </section>

            <section v-show="activeTab === 'security'" class="profile-panel">
              <h2>Bảo mật</h2>
              <p class="profile-panel__desc">Thông tin đăng nhập và bảo mật tài khoản.</p>

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

              <div class="profile-security-tips">
                <h3>Mẹo bảo mật</h3>
                <ul>
                  <li>Không chia sẻ mật khẩu với bất kỳ ai</li>
                  <li>Đăng xuất khi dùng máy công cộng</li>
                  <li>Liên hệ hotline 1900 6750 nếu phát hiện truy cập lạ</li>
                </ul>
              </div>
            </section>
          </div>
        </div>
      </template>
    </div>
  </MainLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import MainLayout from "../layouts/MainLayout.vue";
import ProductImage from "../components/ProductImage.vue";
import { fetchProfileApi, updateProfileApi, uploadProfileAvatarApi } from "../services/api";
import { useAppStore } from "../stores/appStore";
import { normalizeUserRoles, userIsAdmin } from "../utils/roles";
import { userCanAccessPanel } from "../utils/adminAccess";

const router = useRouter();
const store = useAppStore();
const loading = ref(true);
const saving = ref(false);
const editing = ref(false);
const activeTab = ref("account");
const profile = ref({});
const message = ref("");
const error = ref("");
const avatarFile = ref(null);

const form = reactive({
  fullname: "",
  email: "",
  phone: "",
  photo: "",
});

const tabs = [
  { id: "account", label: "Tài khoản", icon: "👤" },
  { id: "orders", label: "Giỏ hàng", icon: "🛍" },
  { id: "security", label: "Bảo mật", icon: "🔒" },
];

const cartItems = computed(() => store.state.cartItems);
const cartCount = computed(() => store.cartCount.value);
const cartAmount = computed(() => store.cartAmount.value);
const favoriteCount = computed(() => store.favoriteCount.value);
const isAdmin = computed(() => userIsAdmin(store.state.user));
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

const roleLabels = computed(() => {
  const roles = normalizeUserRoles(profile.value);
  const map = { ROLE_ADMIN: "Quản trị viên", ROLE_STAFF: "Nhân viên", ROLE_USER: "Khách hàng" };
  return roles.map((r) => map[r] || r.replace("ROLE_", ""));
});

const memberSince = computed(() => {
  const d = profile.value.createDate || profile.value.createdAt;
  if (d) return new Date(d).getFullYear();
  return new Date().getFullYear();
});

const formatPrice = (price) => Number(price || 0).toLocaleString("vi-VN");

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
      fullname: form.fullname.trim(),
      email: form.email.trim(),
      phone: form.phone.trim(),
    };

    if (form.photo && !form.photo.startsWith("blob:")) {
      payload.photo = form.photo.trim();
    }

    data = await updateProfileApi(payload);
    applyProfile(data);
    syncStore();
    editing.value = false;
    message.value = data?.message || "Cập nhật hồ sơ thành công.";
  } catch (err) {
    console.warn("Update profile failed", err);
    error.value =
      err?.response?.data?.message || err?.message || "Không thể cập nhật hồ sơ. Vui lòng thử lại.";
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
});
</script>
