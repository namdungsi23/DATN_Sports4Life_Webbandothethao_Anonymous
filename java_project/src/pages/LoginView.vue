<template>
  <AuthLayout
    title="Đăng nhập"
    subtitle="Chào mừng bạn quay trở lại Sports4Life"
    visual-title="Chinh phục mọi cung đường"
    visual-subtitle="Đăng nhập để theo dõi đơn hàng, nhận ưu đãi và tích điểm thành viên."
    image="https://images.unsplash.com/photo-1460353581641-37baddab0fa2?w=1200&q=80"
    footer-text="Chưa có tài khoản?"
    :footer-link="{ to: '/register', label: 'Đăng ký ngay' }"
  >
    <form class="auth-form" @submit.prevent="submitLogin">
      <div v-if="error" class="auth-form__alert auth-form__alert--error">{{ error }}</div>

      <label class="auth-form__field">
        <span>Tên đăng nhập</span>
        <input v-model="form.username" type="text" placeholder="Nhập username" autocomplete="username" />
      </label>

      <label class="auth-form__field">
        <span>Mật khẩu</span>
        <input v-model="form.pwd" type="password" placeholder="Nhập mật khẩu" autocomplete="current-password" />
      </label>

      <label class="auth-form__check">
        <input id="chk" v-model="form.remember" type="checkbox" />
        <span>Ghi nhớ đăng nhập</span>
      </label>

      <button type="submit" class="auth-form__submit" :disabled="loading">
        {{ loading ? "Đang đăng nhập..." : "Đăng nhập" }}
      </button>

      <a href="/base/oauth2/authorization/google" class="auth-form__google">
        <svg width="18" height="18" viewBox="0 0 24 24"><path fill="#4285F4" d="M22 12c0-.68-.06-1.37-.17-2H12v3.77h5.76A5.26 5.26 0 0 1 12 17.26v3.77c4.74-.22 8.5-4.03 8.5-8.99z"/><path fill="#34A853" d="M12 21c2.43 0 4.47-.8 5.96-2.18l-3.77-2.92c-1.03.69-2.35 1.1-4.19 1.1-3.22 0-5.95-2.17-6.93-5.09H1.06v3.02A10 10 0 0 0 12 21z"/><path fill="#FBBC05" d="M5.07 14.91A6 6 0 0 1 4.6 12c0-1.01.24-1.96.47-2.91V6.07H1.06A10 10 0 0 0 2 12c0 1.61.39 3.14 1.06 4.51l3.01-2.6z"/><path fill="#EA4335" d="M12 5.38c1.32 0 2.5.45 3.44 1.35l2.58-2.58C16.46 2.89 14.42 2 12 2 7.7 2 4.02 4.47 2.06 8.07l3.01 2.6C6.05 7.55 8.78 5.38 12 5.38z"/></svg>
        Đăng nhập bằng Google
      </a>
    </form>
  </AuthLayout>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import AuthLayout from "../layouts/AuthLayout.vue";
import { loginApi } from "../services/api";
import { useAppStore } from "../stores/appStore";
import { resolveDefaultAdminRoute, userCanAccessPanel } from "../utils/adminAccess";

const router = useRouter();
const route = useRoute();
const store = useAppStore();
const loading = ref(false);
const error = ref("");
const form = reactive({ username: "", pwd: "", remember: false });

const resolveAfterLoginPath = () => {
  const redirect = route.query.redirect;
  if (typeof redirect === "string" && redirect.startsWith("/") && !redirect.startsWith("//")) {
    return redirect;
  }
  if (userCanAccessPanel(store.state.user)) {
    return resolveDefaultAdminRoute(store.state.user) || "/admin/dashboard";
  }
  return "/product";
};

const submitLogin = async () => {
  if (!form.username.trim() || !form.pwd.trim()) {
    error.value = "Vui lòng nhập đầy đủ thông tin đăng nhập.";
    return;
  }

  loading.value = true;
  error.value = "";

  try {
    const data = await loginApi({
      username: form.username.trim(),
      password: form.pwd,
      remember: form.remember,
    });

    if (typeof data !== "object" || data === null || !data.username) {
      error.value = "Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin đăng nhập.";
      return;
    }
    if (!data.accessToken) {
      error.value = "Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin đăng nhập.";
      return;
    }

    store.login({
      username: data.username,
      roles: data.roles ?? data.role,
      permissions: data.permissions ?? [],
      panelAccess: data.panelAccess === true,
      accessToken: data.accessToken,
      refreshToken: data.refreshToken,
    });
    await store.loadFavorites();
    router.push(resolveAfterLoginPath());
  } catch (loginError) {
    console.warn("Login failed", loginError);
    error.value =
      loginError?.response?.data?.message ||
      loginError?.message ||
      "Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin đăng nhập.";
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  const q = route.query;
  if (q.oauth_error != null && q.oauth_error !== "") {
    error.value =
      typeof q.oauth_error === "string"
        ? q.oauth_error
        : Array.isArray(q.oauth_error)
          ? q.oauth_error[0]
          : "Đăng nhập Google thất bại.";
    router.replace({ path: "/login" });
    return;
  }
  if (q.oauth === "1" && q.accessToken && q.username) {
    let roles = [];
    try {
      const raw = q.roles;
      if (typeof raw === "string") roles = JSON.parse(raw);
      else if (Array.isArray(raw)) roles = raw;
    } catch {
      roles = [];
    }
    store.login({
      username: String(q.username),
      roles,
      accessToken: String(q.accessToken),
      refreshToken: "",
    });
    store.loadFavorites().finally(() => {
      router.replace(resolveAfterLoginPath());
    });
    return;
  }
});
</script>
