<template>
  <div class="auth-page">
    <div class="auth-page__visual">
      <img :src="image" :alt="title" class="auth-page__bg" />
      <div class="auth-page__overlay">
        <RouterLink to="/" class="auth-page__brand">Sports4Life</RouterLink>
        <div class="auth-page__visual-text">
          <h2>{{ visualTitle }}</h2>
          <p>{{ visualSubtitle }}</p>
        </div>
        <div class="auth-page__brands">
          <span v-for="brand in brands" :key="brand.name" class="auth-page__brand-tag">{{ brand.name }}</span>
        </div>
      </div>
    </div>

    <div class="auth-page__panel">
      <RouterLink to="/" class="auth-page__back">← Về trang chủ</RouterLink>
      <div class="auth-page__form-wrap">
        <h1 class="auth-page__title">{{ title }}</h1>
        <p v-if="subtitle" class="auth-page__subtitle">{{ subtitle }}</p>
        <slot />
        <p v-if="footerLink" class="auth-page__footer-link">
          {{ footerText }}
          <RouterLink :to="footerLink.to">{{ footerLink.label }}</RouterLink>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { RouterLink } from "vue-router";

defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: "" },
  visualTitle: { type: String, default: "Thể thao là phong cách sống" },
  visualSubtitle: {
    type: String,
    default: "Hàng ngàn mẫu giày chính hãng từ Nike, Adidas, Puma và nhiều thương hiệu hàng đầu.",
  },
  image: {
    type: String,
    default: "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=1200&q=80",
  },
  footerText: { type: String, default: "" },
  footerLink: { type: Object, default: null },
});

const brands = [
  { name: "Nike" },
  { name: "Adidas" },
  { name: "Puma" },
  { name: "New Balance" },
  { name: "Converse" },
];
</script>
