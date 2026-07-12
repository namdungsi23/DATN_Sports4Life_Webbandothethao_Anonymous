<template>
  <AuthLayout
    title="Quên mật khẩu"
    :subtitle="stepSubtitle"
    visual-title="Lấy lại quyền truy cập"
    visual-subtitle="Xác minh OTP gửi về email rồi đặt mật khẩu mới."
    image="https://images.unsplash.com/photo-1552346154-21d32810aba3?w=1200&q=80"
    footer-text="Nhớ mật khẩu?"
    :footer-link="{ to: '/login', label: 'Đăng nhập' }"
  >
    <div class="auth-otp-steps" aria-hidden="true">
      <span :class="{ active: step >= 1 }">1. Email</span>
      <span :class="{ active: step >= 2 }">2. OTP</span>
      <span :class="{ active: step >= 3 }">3. Mật khẩu</span>
    </div>

    <form class="auth-form" @submit.prevent="onSubmit">
      <div v-if="error" class="auth-form__alert auth-form__alert--error">{{ error }}</div>
      <div v-if="success" class="auth-form__alert auth-form__alert--success">{{ success }}</div>

      <!-- Step 1: email -->
      <template v-if="step === 1">
        <label class="auth-form__field">
          <span>Email đã đăng ký</span>
          <input
            v-model="form.email"
            type="email"
            placeholder="email@example.com"
            autocomplete="email"
            :class="{ 'is-invalid': fieldErrors.email }"
            @input="clearFieldError('email')"
          />
          <small v-if="fieldErrors.email" class="auth-form__error">{{ fieldErrors.email }}</small>
        </label>
        <button type="submit" class="auth-form__submit" :disabled="loading">
          {{ loading ? "Đang gửi OTP..." : "Gửi mã OTP" }}
        </button>
        <p v-if="devOtp" class="auth-form__dev-link">
          Demo (chưa SMTP): mã OTP là <strong>{{ devOtp }}</strong>
        </p>
      </template>

      <!-- Step 2: OTP -->
      <template v-else-if="step === 2">
        <p class="auth-form__hint">Mã OTP đã gửi tới <strong>{{ form.email }}</strong> (hiệu lực {{ otpTtl }} giây).</p>
        <label class="auth-form__field">
          <span>Mã OTP (6 số)</span>
          <input
            v-model="form.otp"
            type="text"
            inputmode="numeric"
            maxlength="6"
            placeholder="••••••"
            autocomplete="one-time-code"
            class="auth-form__otp-input"
            :class="{ 'is-invalid': fieldErrors.otp }"
            @input="onOtpInput"
          />
          <small v-if="fieldErrors.otp" class="auth-form__error">{{ fieldErrors.otp }}</small>
        </label>
        <button type="submit" class="auth-form__submit" :disabled="loading">
          {{ loading ? "Đang xác minh..." : "Xác minh OTP" }}
        </button>
        <button type="button" class="auth-form__link-btn" :disabled="loading || resendCooldown > 0" @click="resendOtp">
          {{ resendCooldown > 0 ? `Gửi lại sau ${resendCooldown}s` : "Gửi lại OTP" }}
        </button>
        <p v-if="devOtp" class="auth-form__dev-link">
          Demo OTP: <strong>{{ devOtp }}</strong>
        </p>
      </template>

      <!-- Step 3: new password -->
      <template v-else-if="step === 3 && !done">
        <label class="auth-form__field">
          <span>Mật khẩu mới</span>
          <input
            v-model="form.newPassword"
            type="password"
            placeholder="Tối thiểu 8 ký tự, có chữ và số"
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
import { computed, onUnmounted, reactive, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import AuthLayout from "../layouts/AuthLayout.vue";
import { forgotPasswordApi, resetPasswordApi, verifyOtpApi } from "../services/api";
import { useToast } from "../stores/appStore";
import { firstError, getApiError, runValidation } from "../utils/validators";

const router = useRouter();
const toast = useToast();
const step = ref(1);
const loading = ref(false);
const error = ref("");
const success = ref("");
const done = ref(false);
const devOtp = ref("");
const otpTtl = ref(300);
const resetToken = ref("");
const resendCooldown = ref(0);
const fieldErrors = reactive({});
const form = reactive({
  email: "",
  otp: "",
  newPassword: "",
  confirmPassword: "",
});

let cooldownTimer = null;

const stepSubtitle = computed(() => {
  if (step.value === 1) return "Nhập email — hệ thống sẽ gửi mã OTP 6 số";
  if (step.value === 2) return "Nhập mã OTP nhận được trong email";
  return "Tạo mật khẩu mới cho tài khoản";
});

const clearFieldError = (field) => {
  delete fieldErrors[field];
};

const onOtpInput = () => {
  form.otp = String(form.otp || "").replace(/\D/g, "").slice(0, 6);
  clearFieldError("otp");
};

const startCooldown = (sec = 60) => {
  resendCooldown.value = sec;
  if (cooldownTimer) clearInterval(cooldownTimer);
  cooldownTimer = setInterval(() => {
    resendCooldown.value -= 1;
    if (resendCooldown.value <= 0) {
      clearInterval(cooldownTimer);
      cooldownTimer = null;
    }
  }, 1000);
};

onUnmounted(() => {
  if (cooldownTimer) clearInterval(cooldownTimer);
});

const sendOtp = async () => {
  error.value = "";
  success.value = "";
  Object.keys(fieldErrors).forEach((k) => delete fieldErrors[k]);

  const result = runValidation(form, { email: ["required", "email"] });
  if (!result.ok) {
    Object.assign(fieldErrors, result.errors);
    error.value = firstError(result.errors);
    return false;
  }

  loading.value = true;
  try {
    const data = await forgotPasswordApi({ email: result.values.email });
    form.email = String(result.values.email);
    otpTtl.value = Number(data?.otpTtlSeconds) || 300;
    success.value = data?.message || "Nếu email tồn tại, mã OTP đã được gửi.";
    toast.success("Đã gửi yêu cầu OTP.");
    devOtp.value = data?.devOtp ? String(data.devOtp) : "";
    step.value = 2;
    form.otp = "";
    startCooldown(60);
    return true;
  } catch (err) {
    const apiErr = getApiError(err, "Không gửi được OTP.");
    error.value = apiErr.message;
    Object.assign(fieldErrors, apiErr.errors || {});
    toast.error(error.value);
    return false;
  } finally {
    loading.value = false;
  }
};

const verifyOtp = async () => {
  error.value = "";
  success.value = "";
  Object.keys(fieldErrors).forEach((k) => delete fieldErrors[k]);

  if (!form.otp || form.otp.length < 4) {
    fieldErrors.otp = "Nhập đủ mã OTP.";
    error.value = fieldErrors.otp;
    return;
  }

  loading.value = true;
  try {
    const data = await verifyOtpApi({ email: form.email, otp: form.otp });
    if (!data?.resetToken) {
      throw new Error("Không nhận được token đặt lại mật khẩu.");
    }
    resetToken.value = data.resetToken;
    success.value = data.message || "Xác minh OTP thành công.";
    toast.success(success.value);
    step.value = 3;
    form.newPassword = "";
    form.confirmPassword = "";
  } catch (err) {
    const apiErr = getApiError(err, "OTP không hợp lệ.");
    error.value = apiErr.message;
    fieldErrors.otp = apiErr.message;
    toast.error(error.value);
  } finally {
    loading.value = false;
  }
};

const resetPassword = async () => {
  error.value = "";
  success.value = "";
  Object.keys(fieldErrors).forEach((k) => delete fieldErrors[k]);

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
    return;
  }

  loading.value = true;
  try {
    const data = await resetPasswordApi({
      token: resetToken.value,
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

const onSubmit = () => {
  if (step.value === 1) return sendOtp();
  if (step.value === 2) return verifyOtp();
  if (step.value === 3) return resetPassword();
};

const resendOtp = async () => {
  if (resendCooldown.value > 0) return;
  await sendOtp();
};
</script>
