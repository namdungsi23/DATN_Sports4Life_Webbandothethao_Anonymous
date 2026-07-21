<template>
  <MainLayout>
    <template #full>
      <div class="page-hero page-hero--more">
        <img
          src="https://images.unsplash.com/photo-1556906781-9a412961c28c?w=1600&q=80"
          alt="Cửa hàng giày thể thao Sports4Life"
        />
        <div class="page-hero__content site-container">
          <img :src="BRAND.logoMarkUrl" alt="" class="page-hero__logo-mark" width="56" height="56" />
          <p class="page-hero__brand">{{ BRAND.name }}</p>
          <p class="page-hero__eyebrow">Since 2019</p>
          <h1>Về chúng tôi</h1>
          <p>Địa chỉ tin cậy cho người yêu thể thao — giày chính hãng, phong cách hiện đại</p>
        </div>
      </div>
    </template>

    <section class="more-story">
      <div class="more-story__media">
        <img
          src="https://images.unsplash.com/photo-1606107557195-0e29a4b5b4aa?w=1200&q=80"
          alt="Giày chạy bộ chính hãng tại Sports4Life"
        />
      </div>
      <div class="more-story__text">
        <p class="section-head__eyebrow">Câu chuyện</p>
        <h2>Hơn cả một cửa hàng giày</h2>
        <p>
          Sports4Life là hệ thống phân phối giày thể thao chính hãng. Chúng tôi tin rằng đôi giày
          phù hợp sẽ giúp bạn tự tin chinh phục mọi thử thách.
        </p>
        <p>
          Với đội ngũ tư vấn nhiệt tình, chính sách đổi trả linh hoạt và giao hàng toàn quốc,
          Sports4Life cam kết mang đến trải nghiệm mua sắm rõ ràng và đáng tin.
        </p>
        <RouterLink to="/product" class="more-story__btn">Khám phá sản phẩm</RouterLink>
      </div>
    </section>

    <section class="more-promises">
      <div class="section-head section-head--center">
        <p class="section-head__eyebrow">Cam kết</p>
        <h2 class="section-head__title">Vì sao chọn Sports4Life</h2>
      </div>
      <ol class="more-promises__list">
        <li v-for="(item, i) in promises" :key="item.title" class="more-promises__item">
          <span class="more-promises__num">{{ String(i + 1).padStart(2, "0") }}</span>
          <div>
            <h3>{{ item.title }}</h3>
            <p>{{ item.text }}</p>
          </div>
        </li>
      </ol>
    </section>

    <BrandStrip :show-all-link="true" />

    <section class="more-cta-band">
      <img
        src="https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=1400&q=80"
        alt=""
        class="more-cta-band__bg"
      />
      <div class="more-cta-band__content site-container">
        <h2>Bạn cần tư vấn?</h2>
        <p>Đội ngũ Sports4Life sẵn sàng hỗ trợ mỗi ngày</p>
        <div class="more-cta-band__actions">
          <RouterLink to="/contact" class="more-cta-band__btn">Liên hệ ngay</RouterLink>
          <a :href="`tel:${BRAND.phoneTel}`" class="more-cta-band__phone">{{ BRAND.phone }}</a>
        </div>
      </div>
    </section>
  </MainLayout>
</template>

<script setup>
import { RouterLink } from "vue-router";
import MainLayout from "../layouts/MainLayout.vue";
import BrandStrip from "../components/BrandStrip.vue";
import { BRAND } from "../utils/brand";

const promises = [
  {
    title: "100% chính hãng",
    text: "Nguồn gốc xuất xứ rõ ràng, tem mác đầy đủ cho mọi sản phẩm.",
  },
  {
    title: "Giao hàng nhanh",
    text: "Giao toàn quốc 2–5 ngày, freeship đơn từ 499.000đ.",
  },
  {
    title: "Đổi trả linh hoạt",
    text: "Hỗ trợ đổi size trong 7 ngày nếu sản phẩm còn nguyên tem.",
  },
  {
    title: "Thanh toán tiện lợi",
    text: "COD, VNPAY, MoMo — an toàn và rõ ràng từng bước.",
  },
];
</script>
