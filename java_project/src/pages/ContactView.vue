<template>
  <MainLayout>
    <template #full>
      <div class="page-hero page-hero--contact">
        <img src="https://images.unsplash.com/photo-1423666639041-f56000c27a9a?w=1400&q=80" alt="Liên hệ" />
        <div class="page-hero__content site-container">
          <p class="page-hero__eyebrow">Hỗ trợ 24/7</p>
          <h1>Liên hệ với chúng tôi</h1>
          <p>Đội ngũ Sports4Life luôn sẵn sàng lắng nghe và hỗ trợ bạn</p>
        </div>
      </div>
    </template>

    <div class="contact-page">
      <div class="contact-cards">
        <article v-for="card in contactCards" :key="card.title" class="contact-card">
          <span class="contact-card__icon">{{ card.icon }}</span>
          <h3>{{ card.title }}</h3>
          <p v-for="line in card.lines" :key="line">{{ line }}</p>
        </article>
      </div>

      <div class="contact-layout">
        <div class="contact-map">
          <img src="https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=800&q=80" alt="Văn phòng Sports4Life" />
          <div class="contact-map__info">
            <h3>Showroom TP.HCM</h3>
            <p>123 Nguyễn Văn Linh, Quận 7, TP.HCM</p>
            <p>Thứ 2 – CN: 8:00 – 22:00</p>
          </div>
        </div>

        <div class="contact-form-wrap">
          <h2>Gửi tin nhắn</h2>
          <p class="contact-form-wrap__desc">Điền form bên dưới, chúng tôi sẽ phản hồi trong vòng 24 giờ.</p>

          <div v-if="message" class="contact-alert contact-alert--success">{{ message }}</div>
          <div v-if="error" class="contact-alert contact-alert--error">{{ error }}</div>

          <form class="contact-form" @submit.prevent="submitContact">
            <label class="contact-form__field">
              <span>Họ và tên *</span>
              <input
                v-model="form.name"
                type="text"
                placeholder="Nguyễn Văn A"
                :class="{ 'is-invalid': fieldErrors.name }"
                @input="clearFieldError('name')"
              />
              <small v-if="fieldErrors.name" class="contact-form__error">{{ fieldErrors.name }}</small>
            </label>
            <div class="contact-form__row">
              <label class="contact-form__field">
                <span>Email *</span>
                <input
                  v-model="form.email"
                  type="email"
                  placeholder="email@example.com"
                  :class="{ 'is-invalid': fieldErrors.email }"
                  @input="clearFieldError('email')"
                />
                <small v-if="fieldErrors.email" class="contact-form__error">{{ fieldErrors.email }}</small>
              </label>
              <label class="contact-form__field">
                <span>Số điện thoại</span>
                <input
                  v-model="form.phone"
                  type="tel"
                  placeholder="090x xxx xxx"
                  :class="{ 'is-invalid': fieldErrors.phone }"
                  @input="clearFieldError('phone')"
                />
                <small v-if="fieldErrors.phone" class="contact-form__error">{{ fieldErrors.phone }}</small>
              </label>
            </div>
            <label class="contact-form__field">
              <span>Nội dung *</span>
              <textarea
                v-model="form.message"
                rows="5"
                placeholder="Bạn cần hỗ trợ gì?"
                :class="{ 'is-invalid': fieldErrors.message }"
                @input="clearFieldError('message')"
              />
              <small v-if="fieldErrors.message" class="contact-form__error">{{ fieldErrors.message }}</small>
            </label>
            <button type="submit" class="contact-form__submit" :disabled="loading">
              {{ loading ? "Đang gửi..." : "Gửi liên hệ" }}
            </button>
          </form>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script setup>
import { reactive, ref } from "vue";
import MainLayout from "../layouts/MainLayout.vue";
import { sendContactApi } from "../services/api";
import { useToast } from "../stores/appStore";
import { firstError, getApiError, normalizePhone, runValidation } from "../utils/validators";

const toast = useToast();
const loading = ref(false);
const error = ref("");
const message = ref("");
const fieldErrors = reactive({});
const form = reactive({ name: "", email: "", phone: "", message: "" });

const contactCards = [
  { icon: "📞", title: "Hotline", lines: ["0336 694 988", "Hỗ trợ 8:00 – 22:00 hàng ngày"] },
  { icon: "✉", title: "Email", lines: ["support@sports4life.vn", "Phản hồi trong 24h"] },
  { icon: "📍", title: "Địa chỉ", lines: ["123 Nguyễn Văn Linh, Q.7", "TP. Hồ Chí Minh"] },
  { icon: "💬", title: "Mạng xã hội", lines: ["Facebook / Instagram", "TikTok: @sports4life"] },
];

const clearFieldError = (key) => {
  delete fieldErrors[key];
};

const submitContact = async () => {
  Object.keys(fieldErrors).forEach((k) => delete fieldErrors[k]);
  const result = runValidation(form, {
    name: [
      "required",
      { type: "min", min: 2, message: "Họ tên tối thiểu 2 ký tự." },
      { type: "max", max: 100 },
    ],
    email: ["required", "email"],
    phone: ["phone"],
    message: [
      "required",
      { type: "min", min: 10, message: "Nội dung tối thiểu 10 ký tự." },
      { type: "max", max: 2000 },
    ],
  });

  if (!result.ok) {
    Object.assign(fieldErrors, result.errors);
    error.value = firstError(result.errors);
    message.value = "";
    toast.error(error.value);
    return;
  }

  loading.value = true;
  error.value = "";
  message.value = "";

  try {
    await sendContactApi({
      name: result.values.name,
      email: result.values.email,
      phone: form.phone ? normalizePhone(form.phone) : "",
      message: result.values.message,
    });
    message.value = "Gửi liên hệ thành công. Chúng tôi sẽ phản hồi sớm nhất!";
    toast.success(message.value);
    form.name = "";
    form.email = "";
    form.phone = "";
    form.message = "";
  } catch (contactError) {
    console.warn("Contact failed", contactError);
    const api = getApiError(
      contactError,
      "Không gửi được liên hệ. Vui lòng thử lại hoặc gọi hotline 0336 694 988."
    );
    Object.assign(fieldErrors, api.errors);
    error.value = api.message;
    toast.error(error.value);
  } finally {
    loading.value = false;
  }
};
</script>
