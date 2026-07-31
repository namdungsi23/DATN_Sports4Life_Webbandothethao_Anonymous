<template>
  <AuthLayout
    title="Đăng ký tài khoản"
    :subtitle="stepSubtitle"
    visual-title="Bắt đầu hành trình thể thao"
    visual-subtitle="Tạo tài khoản để mua sắm nhanh hơn và theo dõi đơn hàng."
    image="https://images.unsplash.com/photo-1571902943202-507ec2618e8f?w=1200&q=80"
    footer-text="Đã có tài khoản?"
    :footer-link="{ to: '/login', label: 'Đăng nhập' }"
  >
    <form class="auth-form" @submit.prevent="onSubmit">
      <div v-if="error" class="auth-form__alert auth-form__alert--error">{{ error }}</div>
      <div v-if="success" class="auth-form__alert auth-form__alert--success">{{ success }}</div>

      <!-- Step 1: account info + channel -->
      <template v-if="step === 1">
        <label class="auth-form__field">
          <span>Tên đăng nhập *</span>
          <input
            v-model="form.username"
            type="text"
            placeholder="Tên đăng nhập"
            autocomplete="username"
            :class="{ 'is-invalid': fieldErrors.username }"
            @input="clearFieldError('username')"
          />
          <small v-if="fieldErrors.username" class="auth-form__error">{{ fieldErrors.username }}</small>
        </label>

        <label class="auth-form__field">
          <span>Họ và tên *</span>
          <input
            v-model="form.fullname"
            type="text"
            placeholder="Họ và tên"
            autocomplete="name"
            :class="{ 'is-invalid': fieldErrors.fullname }"
            @input="clearFieldError('fullname')"
          />
          <small v-if="fieldErrors.fullname" class="auth-form__error">{{ fieldErrors.fullname }}</small>
        </label>

        <label class="auth-form__field">
          <span>Email *</span>
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

        <div class="auth-form__row">
          <label class="auth-form__field">
            <span>Mật khẩu *</span>
            <input
              v-model="form.password"
              type="password"
              placeholder="Mật khẩu"
              autocomplete="new-password"
              :class="{ 'is-invalid': fieldErrors.password }"
              @input="clearFieldError('password')"
            />
            <small v-if="fieldErrors.password" class="auth-form__error">{{ fieldErrors.password }}</small>
          </label>
          <label class="auth-form__field">
            <span>Nhập lại mật khẩu *</span>
            <input
              v-model="form.confirm"
              type="password"
              placeholder="Nhập lại mật khẩu"
              autocomplete="new-password"
              :class="{ 'is-invalid': fieldErrors.confirm }"
              @input="clearFieldError('confirm')"
            />
            <small v-if="fieldErrors.confirm" class="auth-form__error">{{ fieldErrors.confirm }}</small>
          </label>
        </div>

        <fieldset class="auth-channel">
          <legend>Xác minh danh tính *</legend>
          <div class="auth-channel__options">
            <label class="auth-channel__option" :class="{ active: form.verifyChannel === 'EMAIL' }">
              <input v-model="form.verifyChannel" type="radio" value="EMAIL" @change="clearFieldError('verifyChannel')" />
              <span class="auth-channel__title">Gmail</span>
            </label>
            <label class="auth-channel__option" :class="{ active: form.verifyChannel === 'SMS' }">
              <input v-model="form.verifyChannel" type="radio" value="SMS" @change="clearFieldError('verifyChannel')" />
              <span class="auth-channel__title">SMS</span>
            </label>
          </div>
          <small v-if="fieldErrors.verifyChannel" class="auth-form__error">{{ fieldErrors.verifyChannel }}</small>
        </fieldset>

        <label v-if="form.verifyChannel === 'SMS'" class="auth-form__field">
          <span>Số điện thoại *</span>
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

        <label class="auth-form__upload">
          <span>Ảnh đại diện (tuỳ chọn — lưu Cloudinary)</span>
          <input type="file" accept="image/*" @change="onFileChange" />
          <img v-if="photoPreview" :src="photoPreview" alt="Preview" class="auth-form__avatar-preview" />
          <small v-if="fieldErrors.photo" class="auth-form__error">{{ fieldErrors.photo }}</small>
        </label>

        <button type="submit" class="auth-form__submit auth-form__submit--register" :disabled="loading">
          {{ loading ? "Đang gửi OTP..." : "Tiếp tục" }}
        </button>
      </template>

      <!-- Step 2: OTP -->
      <template v-else>
        <p v-if="otpMeta.destination" class="auth-form__hint">
          Mã đã gửi tới <strong>{{ otpMeta.destination }}</strong>
          <span v-if="otpMeta.channel === 'EMAIL'"> — kiểm tra hộp thư / Spam.</span>
        </p>
        <div v-if="otpMeta.devOtp" class="auth-form__alert auth-form__alert--success">
          <div>{{ otpMeta.devNote || "OTP demo (SMS/SMTP chưa gửi thật):" }}</div>
          <div v-if="otpMeta.smsError" class="auth-form__hint">{{ otpMeta.smsError }}</div>
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
        <button type="submit" class="auth-form__submit auth-form__submit--register" :disabled="loading">
          {{ loading ? "Đang xác minh..." : "Xác minh" }}
        </button>
        <button type="button" class="auth-form__link-btn" :disabled="loading || resendCooldown > 0" @click="resendOtp">
          {{ resendCooldown > 0 ? `Gửi lại sau ${resendCooldown}s` : "Gửi lại OTP" }}
        </button>
        <button type="button" class="auth-form__link-btn" :disabled="loading" @click="backToForm">
          Quay lại
        </button>
      </template>
    </form>
  </AuthLayout>
</template>


<script setup>
import { computed, onUnmounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import AuthLayout from "../layouts/AuthLayout.vue";
import { registerApi, registerResendOtpApi, registerVerifyOtpApi } from "../services/api";
import { useToast } from "../stores/appStore";
import { firstError, getApiError, runValidation } from "../utils/validators";

const router = useRouter();
const toast = useToast();
const step = ref(1);
const error = ref("");
const success = ref("");
const loading = ref(false);
const fieldErrors = reactive({});
const resendCooldown = ref(0);
const form = reactive({
  username: "",
  fullname: "",
  email: "",
  password: "",
  confirm: "",
  photo: null,
  verifyChannel: "EMAIL",
  phone: "",
  otp: "",
});
const photoPreview = ref("");
const otpMeta = reactive({
  channel: "",
  destination: "",
  devOtp: "",
  devNote: "",
  smsError: "",
});

let cooldownTimer = null;

const stepSubtitle = computed(() =>
  step.value === 1 ? "Tạo tài khoản Sports4Life" : "Xác minh tài khoản"
);

const clearFieldError = (key) => {
  delete fieldErrors[key];
};

const onFileChange = (e) => {
  const file = e.target.files?.[0] || null;
  if (photoPreview.value) URL.revokeObjectURL(photoPreview.value);
  form.photo = file;
  photoPreview.value = file ? URL.createObjectURL(file) : "";
  clearFieldError("photo");
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
  if (photoPreview.value) URL.revokeObjectURL(photoPreview.value);
});

const backToForm = () => {
  step.value = 1;
  form.otp = "";
  error.value = "";
  success.value = "";
  otpMeta.channel = "";
  otpMeta.destination = "";
  otpMeta.devOtp = "";
  otpMeta.devNote = "";
  otpMeta.smsError = "";
};

const applyOtpResponse = (data) => {
  success.value = data?.message || "Đã gửi mã OTP.";
  otpMeta.channel = data?.verifyChannel || form.verifyChannel || "";
  otpMeta.destination = data?.destination || "";
  otpMeta.devOtp = data?.devOtp ? String(data.devOtp) : "";
  otpMeta.devNote = data?.devNote || "";
  otpMeta.smsError = data?.smsError || "";
  step.value = 2;
  // Demo/SMS: điền sẵn OTP để xác minh được khi gateway/SMTP chưa gửi thật
  form.otp = otpMeta.devOtp || "";
  startCooldown(60);
};

const submitRegister = async () => {
  Object.keys(fieldErrors).forEach((k) => delete fieldErrors[k]);
  const rules = {
    username: ["required", "username"],
    fullname: [
      "required",
      { type: "min", min: 2, message: "Họ tên tối thiểu 2 ký tự." },
      { type: "max", max: 100 },
    ],
    email: ["required", "email", { type: "max", max: 100 }],
    password: ["required", "password"],
    confirm: [
      "required",
      { type: "match", field: "password", message: "Mật khẩu nhập lại không khớp." },
    ],
    photo: ["fileImage"],
  };
  if (form.verifyChannel === "SMS") {
    rules.phone = ["required", "phone"];
  }

  const result = runValidation(form, rules);
  if (!form.verifyChannel) {
    fieldErrors.verifyChannel = "Chọn Gmail hoặc SMS để xác minh.";
  }
  if (!result.ok || fieldErrors.verifyChannel) {
    Object.assign(fieldErrors, result.errors);
    error.value = firstError(fieldErrors) || firstError(result.errors);
    success.value = "";
    toast.error(error.value);
    return;
  }

  loading.value = true;
  error.value = "";
  success.value = "";
  try {
    const data = await registerApi({
      username: result.values.username,
      fullname: result.values.fullname,
      email: result.values.email,
      password: form.password,
      verifyChannel: form.verifyChannel,
      phone: form.verifyChannel === "SMS" ? result.values.phone : undefined,
      photo: form.photo || undefined,
    });
    applyOtpResponse(data);
    toast.success("Đã gửi mã OTP xác minh.");
  } catch (registerError) {
    const api = getApiError(registerError, "Đăng ký thất bại. Vui lòng thử lại.");
    Object.assign(fieldErrors, api.errors);
    error.value = api.message;
    toast.error(error.value);
  } finally {
    loading.value = false;
  }
};

const submitVerify = async () => {
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
  error.value = "";
  success.value = "";
  try {
    const data = await registerVerifyOtpApi({
      username: form.username.trim(),
      verifyChannel: form.verifyChannel,
      email: form.email.trim(),
      phone: form.verifyChannel === "SMS" ? form.phone.trim() : undefined,
      otp: form.otp,
    });
    success.value = data?.message || "Xác minh thành công. Bạn có thể đăng nhập.";
    toast.success(success.value);
    setTimeout(() => router.push("/login"), 1200);
  } catch (verifyError) {
    const api = getApiError(verifyError, "Xác minh OTP thất bại.");
    Object.assign(fieldErrors, api.errors);
    error.value = api.message;
    toast.error(error.value);
  } finally {
    loading.value = false;
  }
};

const resendOtp = async () => {
  if (resendCooldown.value > 0) return;
  loading.value = true;
  error.value = "";
  try {
    const data = await registerResendOtpApi({
      username: form.username.trim(),
      verifyChannel: form.verifyChannel,
      email: form.email.trim(),
      phone: form.verifyChannel === "SMS" ? form.phone.trim() : undefined,
    });
    applyOtpResponse(data);
    toast.success("Đã gửi lại OTP.");
  } catch (err) {
    const api = getApiError(err, "Không gửi lại được OTP.");
    error.value = api.message;
    toast.error(error.value);
  } finally {
    loading.value = false;
  }
};

const onSubmit = () => {
  if (step.value === 1) return submitRegister();
  return submitVerify();
};
</script>
