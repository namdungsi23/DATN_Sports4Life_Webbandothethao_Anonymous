<template>
  <AuthLayout
    title="Quên mật khẩu"
    :subtitle="stepSubtitle"
    visual-title="Lấy lại quyền truy cập"
    visual-subtitle="Xác minh tài khoản rồi đặt mật khẩu mới."
    image="https://images.unsplash.com/photo-1552346154-21d32810aba3?w=1200&q=80"
    footer-text="Nhớ mật khẩu?"
    :footer-link="{ to: '/login', label: 'Đăng nhập' }"
  >
    <form class="auth-form" @submit.prevent="onSubmit">
      <div v-if="error" class="auth-form__alert auth-form__alert--error">{{ error }}</div>
      <div v-if="success" class="auth-form__alert auth-form__alert--success">{{ success }}</div>

      <!-- Step 1: channel + email/phone -->
      <template v-if="step === 1">
        <fieldset class="auth-channel">
          <legend>Xác nhận danh tính *</legend>
          <div class="auth-channel__options">
            <label class="auth-channel__option" :class="{ active: form.verifyChannel === 'EMAIL' }">
              <input v-model="form.verifyChannel" type="radio" value="EMAIL" @change="onChannelChange" />
              <span class="auth-channel__title">Gmail</span>
            </label>
            <label class="auth-channel__option" :class="{ active: form.verifyChannel === 'SMS' }">
              <input v-model="form.verifyChannel" type="radio" value="SMS" @change="onChannelChange" />
              <span class="auth-channel__title">SMS</span>
            </label>
          </div>
        </fieldset>

        <label v-if="form.verifyChannel === 'EMAIL'" class="auth-form__field">
          <span>Email đã đăng ký</span>
          <input
            v-model="form.email"
            type="email"
            placeholder="Email"
            autocomplete="email"
            :class="{ 'is-invalid': fieldErrors.email }"
            @input="clearFieldError('email')"
          />
          <small v-if="fieldErrors.email" class="auth-form__error">{{ fieldErrors.email }}</small>
        </label>

        <label v-else class="auth-form__field">
          <span>Số điện thoại đã đăng ký</span>
          <input
            v-model="form.phone"
            type="tel"
            placeholder="Số điện thoại"
            autocomplete="tel"
            :class="{ 'is-invalid': fieldErrors.phone }"
            @input="clearFieldError('phone')"
          />
          <small v-if="fieldErrors.phone" class="auth-form__error">{{ fieldErrors.phone }}</small>
        </label>

        <button type="submit" class="auth-form__submit" :disabled="loading">
          {{ loading ? "Đang gửi OTP..." : "Gửi mã OTP" }}
        </button>
      </template>

      <!-- Step 2: OTP -->
      <template v-else-if="step === 2">
        <p v-if="otpMeta.destination" class="auth-form__hint">
          Mã đã gửi tới <strong>{{ otpMeta.destination }}</strong>
        </p>
        <div v-if="otpMeta.devOtp" class="auth-form__alert auth-form__alert--success">
          <div>{{ otpMeta.devNote || "OTP demo (SMS/SMTP chưa gửi thật):" }}</div>
          <div class="auth-form__otp-demo">{{ otpMeta.devOtp }}</div>
        </div>
        <label class="auth-form__field">
          <span>Mã OTP</span>
          <input
            v-model="form.otp"
            type="text"
            inputmode="numeric"
            maxlength="6"
            placeholder="Nhập mã OTP"
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
        <button type="button" class="auth-form__link-btn" :disabled="loading" @click="backToStep1">
          Quay lại
        </button>
      </template>

      <!-- Step 3: new password -->
      <template v-else-if="step === 3 && !done">
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
const resetToken = ref("");
const resendCooldown = ref(0);
const fieldErrors = reactive({});
const form = reactive({
  verifyChannel: "EMAIL",
  email: "",
  phone: "",
  otp: "",
  newPassword: "",
  confirmPassword: "",
});
const otpMeta = reactive({
  destination: "",
  devOtp: "",
  devNote: "",
});

let cooldownTimer = null;

const stepSubtitle = computed(() => {
  if (step.value === 1) return "Xác minh tài khoản để đặt lại mật khẩu";
  if (step.value === 2) return "Nhập mã OTP đã nhận";
  return "Tạo mật khẩu mới";
});

const clearFieldError = (field) => {
  delete fieldErrors[field];
};

const onChannelChange = () => {
  clearFieldError("email");
  clearFieldError("phone");
  error.value = "";
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

const backToStep1 = () => {
  step.value = 1;
  form.otp = "";
  error.value = "";
  success.value = "";
  otpMeta.destination = "";
  otpMeta.devOtp = "";
  otpMeta.devNote = "";
};

const applyOtpResponse = (data) => {
  success.value = data?.message || "Nếu thông tin tồn tại, mã OTP đã được gửi.";
  otpMeta.destination = data?.destination || "";
  otpMeta.devOtp = data?.devOtp ? String(data.devOtp) : "";
  otpMeta.devNote = data?.devNote || "";
  step.value = 2;
  form.otp = otpMeta.devOtp || "";
  startCooldown(60);
};

const sendOtp = async () => {
  error.value = "";
  success.value = "";
  Object.keys(fieldErrors).forEach((k) => delete fieldErrors[k]);

  const rules =
    form.verifyChannel === "SMS"
      ? { phone: ["required", "phone"] }
      : { email: ["required", "email"] };
  const result = runValidation(form, rules);
  if (!result.ok) {
    Object.assign(fieldErrors, result.errors);
    error.value = firstError(result.errors);
    toast.error(error.value);
    return false;
  }

  loading.value = true;
  try {
    const payload = {
      verifyChannel: form.verifyChannel,
      email: form.verifyChannel === "EMAIL" ? result.values.email : undefined,
      phone: form.verifyChannel === "SMS" ? result.values.phone : undefined,
    };
    const data = await forgotPasswordApi(payload);
    if (form.verifyChannel === "EMAIL") form.email = String(result.values.email);
    else form.phone = String(result.values.phone);
    applyOtpResponse(data);
    toast.success(otpMeta.devOtp ? "OTP demo đã hiện trên form." : "Đã gửi yêu cầu OTP.");
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

  const result = runValidation(form, {
    otp: [
      "required",
      { type: "min", min: 6, message: "Mã OTP gồm 6 số." },
      { type: "max", max: 6, message: "Mã OTP gồm 6 số." },
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
    const data = await verifyOtpApi({
      verifyChannel: form.verifyChannel,
      email: form.verifyChannel === "EMAIL" ? form.email : undefined,
      phone: form.verifyChannel === "SMS" ? form.phone : undefined,
      otp: form.otp,
    });
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
    toast.error(error.value);
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
