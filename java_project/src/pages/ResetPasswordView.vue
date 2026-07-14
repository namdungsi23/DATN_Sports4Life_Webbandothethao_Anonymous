<template>
  <AuthLayout
    title="Đặt lại mật khẩu"
    subtitle="Nhập mật khẩu mới cho tài khoản"
    visual-title="Bảo mật tài khoản"
    visual-subtitle="Đặt mật khẩu mới để tiếp tục sử dụng Sports4Life."
    image="https://images.unsplash.com/photo-1606107557195-0f29cb4c6adc?w=1200&q=80"
    footer-text="Chưa có OTP?"
    :footer-link="{ to: '/forgot-password', label: 'Quên mật khẩu' }"
  >
    <form class="auth-form" @submit.prevent="submitReset">
      <div v-if="!token" class="auth-form__alert auth-form__alert--error">
        Thiếu phiên đặt lại. Vui lòng bắt đầu từ
        <RouterLink to="/forgot-password">quên mật khẩu</RouterLink>.
      </div>
      <div v-if="error" class="auth-form__alert auth-form__alert--error">{{ error }}</div>
      <div v-if="success" class="auth-form__alert auth-form__alert--success">{{ success }}</div>

      <template v-if="token && !done">
        <label class="auth-form__field">
          <span>Mật khẩu mới</span>
          <input
            v-model="form.newPassword"
            type="password"
            placeholder="Mật khẩu mới"
            autocomplete="new-password"
            :class="{ 'is-invalid': fieldErrors.newPassword }"
            @input="clearFieldError('newPassword')"
          />
          <small v-if="fieldErrors.newPassword" class="auth-form__error">{{ fieldErrors.newPassword }}</small>
        </label>

        <label class="auth-form__field">
          <span>Xác nhận mật khẩu</span>
          <input
            v-model="form.confirmPassword"
            type="password"
            placeholder="Nhập lại mật khẩu"
            autocomplete="new-password"
            :class="{ 'is-invalid': fieldErrors.confirmPassword }"
            @input="clearFieldError('confirmPassword')"
          />
          <small v-if="fieldErrors.confirmPassword" class="auth-form__error">{{
            fieldErrors.confirmPassword
          }}</small>
        </label>

        <button type="submit" class="auth-form__submit" :disabled="loading">
          {{ loading ? "Đang lưu..." : "Đặt lại mật khẩu" }}
        </button>
      </template>

      <RouterLink v-if="done" to="/login" class="auth-form__submit" style="text-align: center; display: block">
        Đăng nhập ngay
      </RouterLink>
    </form>
  </AuthLayout>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import AuthLayout from "../layouts/AuthLayout.vue";
import { resetPasswordApi } from "../services/api";
import { useToast } from "../stores/appStore";
import { firstError, getApiError, runValidation } from "../utils/validators";

const route = useRoute();
const router = useRouter();
const toast = useToast();
const loading = ref(false);
const error = ref("");
const success = ref("");
const done = ref(false);
const token = ref("");
const fieldErrors = reactive({});
const form = reactive({ newPassword: "", confirmPassword: "" });

const clearFieldError = (field) => {
  delete fieldErrors[field];
};

onMounted(() => {
  const t = route.query.token;
  token.value = typeof t === "string" ? t : Array.isArray(t) ? String(t[0] || "") : "";
});

const submitReset = async () => {
  error.value = "";
  success.value = "";
  Object.keys(fieldErrors).forEach((k) => delete fieldErrors[k]);

  if (!token.value) {
    error.value = "Thiếu token đặt lại mật khẩu.";
    return;
  }

  const result = runValidation(form, {
    newPassword: ["required", "password"],
    confirmPassword: [
      "required",
      { type: "match", field: "newPassword", message: "Mật khẩu nhập lại không khớp." },
    ],
  });
  if (!result.ok) {
    Object.assign(fieldErrors, result.errors);
    error.value = firstError(result.errors);
    toast.error(error.value);
    return;
  }

  loading.value = true;
  try {
    const data = await resetPasswordApi({
      token: token.value,
      newPassword: form.newPassword,
      confirmPassword: form.confirmPassword,
    });
    done.value = true;
    success.value = data?.message || "Đặt lại mật khẩu thành công.";
    toast.success(success.value);
    setTimeout(() => router.push("/login"), 1500);
  } catch (err) {
    const apiErr = getApiError(err, "Không đặt lại được mật khẩu.");
    error.value = apiErr.message;
    Object.assign(fieldErrors, apiErr.errors || {});
    toast.error(error.value);
  } finally {
    loading.value = false;
  }
};
</script>
