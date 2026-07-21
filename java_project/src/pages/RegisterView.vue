<template>
  <AuthLayout
    title="Đăng ký tài khoản"
    subtitle="Tham gia cộng đồng Sports4Life ngay hôm nay"
    visual-title="Bắt đầu hành trình thể thao"
    visual-subtitle="Tạo tài khoản để mua sắm nhanh hơn, theo dõi đơn hàng và nhận ưu đãi độc quyền."
    image="https://images.unsplash.com/photo-1571902943202-507ec2618e8f?w=1200&q=80"
    footer-text="Đã có tài khoản?"
    :footer-link="{ to: '/login', label: 'Đăng nhập' }"
  >
    <form class="auth-form" @submit.prevent="submitRegister">
      <div v-if="error" class="auth-form__alert auth-form__alert--error">{{ error }}</div>
      <div v-if="success" class="auth-form__alert auth-form__alert--success">{{ success }}</div>

      <label class="auth-form__field">
        <span>Tên đăng nhập *</span>
        <input
          v-model="form.username"
          type="text"
          placeholder="Username"
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
          placeholder="Nguyễn Văn A"
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
          placeholder="email@example.com"
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
            placeholder="••••••••"
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
            placeholder="••••••••"
            :class="{ 'is-invalid': fieldErrors.confirm }"
            @input="clearFieldError('confirm')"
          />
          <small v-if="fieldErrors.confirm" class="auth-form__error">{{ fieldErrors.confirm }}</small>
        </label>
      </div>

      <label class="auth-form__upload">
        <span>Ảnh đại diện (tuỳ chọn)</span>
        <input type="file" accept="image/*" @change="onFileChange" />
        <small v-if="fieldErrors.photo" class="auth-form__error">{{ fieldErrors.photo }}</small>
      </label>

      <button type="submit" class="auth-form__submit auth-form__submit--register" :disabled="loading">
        {{ loading ? "Đang xử lý..." : "Tạo tài khoản" }}
      </button>
    </form>
  </AuthLayout>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import AuthLayout from "../layouts/AuthLayout.vue";
import { registerApi } from "../services/api";
import { useToast } from "../stores/appStore";
import { firstError, getApiError, runValidation } from "../utils/validators";

const router = useRouter();
const toast = useToast();
const error = ref("");
const success = ref("");
const loading = ref(false);
const fieldErrors = reactive({});
const form = reactive({
  username: "",
  fullname: "",
  email: "",
  password: "",
  confirm: "",
  photo: null,
});

const clearFieldError = (key) => {
  delete fieldErrors[key];
};

const onFileChange = (e) => {
  form.photo = e.target.files?.[0] || null;
  clearFieldError("photo");
};

const submitRegister = async () => {
  Object.keys(fieldErrors).forEach((k) => delete fieldErrors[k]);
  const result = runValidation(form, {
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
  });

  if (!result.ok) {
    Object.assign(fieldErrors, result.errors);
    error.value = firstError(result.errors);
    success.value = "";
    toast.error(error.value);
    return;
  }

  loading.value = true;
  error.value = "";
  success.value = "";

  try {
    await registerApi({
      username: result.values.username,
      fullname: result.values.fullname,
      email: result.values.email,
      password: form.password,
    });
    success.value = "Đăng ký thành công. Bạn có thể đăng nhập ngay.";
    toast.success(success.value);
    setTimeout(() => router.push("/login"), 1200);
  } catch (registerError) {
    console.warn("Register failed", registerError);
    error.value = api.message;
    toast.error(error.value);
  } finally {
    loading.value = false;
  }
};
</script>
