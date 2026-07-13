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
        <span>Tên đăng nhập</span>
        <input v-model="form.username" type="text" placeholder="Username" required />
      </label>

      <label class="auth-form__field">
        <span>Họ và tên</span>
        <input v-model="form.fullname" type="text" placeholder="Nguyễn Văn A" required />
      </label>

      <label class="auth-form__field">
        <span>Email</span>
        <input v-model="form.email" type="email" placeholder="email@example.com" required />
      </label>

      <div class="auth-form__row">
        <label class="auth-form__field">
          <span>Mật khẩu</span>
          <input v-model="form.password" type="password" placeholder="••••••••" required />
        </label>
        <label class="auth-form__field">
          <span>Nhập lại mật khẩu</span>
          <input v-model="form.confirm" type="password" placeholder="••••••••" required />
        </label>
      </div>

      <label class="auth-form__upload">
        <span>Ảnh đại diện (tuỳ chọn)</span>
        <input type="file" accept="image/*" @change="onFileChange" />
      </label>

      <button type="submit" class="auth-form__submit auth-form__submit--register" :disabled="loading">
        {{ loading ? "Đang xử lý..." : "Tạo tài khoản" }}
      </button>
    </form>
  </AuthLayout>
</template>

<script setup>
import { reactive, ref } from "vue";
import AuthLayout from "../layouts/AuthLayout.vue";
import { registerApi } from "../services/api";

const error = ref("");
const success = ref("");
const loading = ref(false);
const form = reactive({ username: "", fullname: "", email: "", password: "", confirm: "", photo: null });

const onFileChange = (e) => {
  form.photo = e.target.files?.[0] || null;
};

const submitRegister = async () => {
  if (form.password !== form.confirm) {
    error.value = "Mật khẩu không khớp.";
    return;
  }

  loading.value = true;
  error.value = "";
  success.value = "";

  try {
    await registerApi({
      username: form.username.trim(),
      fullname: form.fullname.trim(),
      email: form.email.trim(),
      password: form.password,
    });
    success.value = "Đăng ký thành công. Bạn có thể đăng nhập ngay.";
  } catch (registerError) {
    console.warn("Register failed", registerError);
    error.value =
      registerError?.response?.data?.message ||
      registerError?.response?.data?.detail ||
      registerError?.message ||
      "Đăng ký thất bại. Vui lòng thử lại.";
  } finally {
    loading.value = false;
  }
};
</script>
