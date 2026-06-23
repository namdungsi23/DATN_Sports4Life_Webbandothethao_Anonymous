<template>
  <MainLayout>
    <template #full>
      <div class="page-hero page-hero--more">
        <img src="https://images.unsplash.com/photo-1483721310020-033aa2247bd8?w=1400&q=80" alt="Sports4Life Store" />
        <div class="page-hero__content site-container">
          <p class="page-hero__eyebrow">Since 2019</p>
          <h1>Về Sports4Life</h1>
          <p>Địa chỉ tin cậy cho người yêu thể thao — giày chính hãng, phong cách hiện đại</p>
        </div>
      </div>
    </template>

    <section class="more-about">
      <div class="more-about__grid">
        <div class="more-about__images">
          <img class="more-about__img-main" src="https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=700&q=80" alt="Giày thể thao" />
          <img class="more-about__img-sub" src="https://images.unsplash.com/photo-1606107557195-0f29cb4c6adc?w=400&q=80" alt="Adidas" />
        </div>
        <div class="more-about__text">
          <p class="section-head__eyebrow">Câu chuyện của chúng tôi</p>
          <h2>Hơn cả một cửa hàng giày</h2>
          <p>
            Sports4Life là hệ thống phân phối giày thể thao chính hãng. Chúng tôi tin rằng đôi giày
            phù hợp sẽ giúp bạn tự tin chinh phục mọi thử thách.
          </p>
          <p>
            Với đội ngũ tư vấn nhiệt tình, chính sách đổi trả linh hoạt và giao hàng toàn quốc,
            Sports4Life cam kết mang đến trải nghiệm mua sắm tuyệt vời nhất.
          </p>
          <RouterLink to="/product" class="more-about__btn">Khám phá sản phẩm</RouterLink>
        </div>
      </div>
    </section>

    <BrandStrip />

    <section class="more-stats">
      <div class="more-stats__grid">
        <div v-for="stat in stats" :key="stat.label" class="more-stats__item">
          <span class="more-stats__icon">{{ stat.icon }}</span>
          <strong>{{ stat.value }}</strong>
          <span>{{ stat.label }}</span>
        </div>
      </div>
    </section>

    <section class="more-gallery">
      <div class="section-head section-head--center">
        <p class="section-head__eyebrow">Không gian cửa hàng</p>
        <h2 class="section-head__title">Trải nghiệm mua sắm</h2>
      </div>
      <div class="more-gallery__grid">
        <figure v-for="(img, i) in gallery" :key="i">
          <img :src="img.src" :alt="img.alt" loading="lazy" />
          <figcaption>{{ img.caption }}</figcaption>
        </figure>
      </div>
    </section>

    <section class="more-values">
      <div class="more-values__grid">
        <article v-for="item in values" :key="item.title" class="more-values__card">
          <div class="more-values__icon">{{ item.icon }}</div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.text }}</p>
        </article>
      </div>
    </section>

    <section class="more-cta">
      <img src="https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=1200&q=80" alt="" class="more-cta__bg" />
      <div class="more-cta__content">
        <h2>Bạn cần tư vấn?</h2>
        <p>Đội ngũ Sports4Life sẵn sàng hỗ trợ 24/7</p>
        <div class="more-cta__actions">
          <RouterLink to="/contact" class="more-cta__btn">Liên hệ ngay</RouterLink>
          <a href="tel:19006750" class="more-cta__phone">📞 1900 6750</a>
        </div>
      </div>
    </section>
  </MainLayout>
</template>

<script setup>
import { RouterLink } from "vue-router";
import MainLayout from "../layouts/MainLayout.vue";
import BrandStrip from "../components/BrandStrip.vue";

const stats = [
  { icon: "👥", value: "10K+", label: "Khách hàng tin tưởng" },
  { icon: "👟", value: "500+", label: "Mẫu giày chính hãng" },
  { icon: "🏆", value: "6+", label: "Thương hiệu đối tác" },
  { icon: "⏰", value: "24/7", label: "Hỗ trợ khách hàng" },
];

const gallery = [
  { src: "https://images.unsplash.com/photo-1556906781-95a648118443?w=600&q=80", alt: "Cửa hàng", caption: "Showroom hiện đại" },
  { src: "https://images.unsplash.com/photo-1571902943202-507ec2618e8f?w=600&q=80", alt: "Phòng gym", caption: "Không gian thể thao" },
  { src: "https://images.unsplash.com/photo-1460353581641-37baddab0fa2?w=600&q=80", alt: "Chạy bộ", caption: "Phong cách active" },
  { src: "https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?w=600&q=80", alt: "Sneaker", caption: "Bộ sưu tập sneaker" },
];

const values = [
  { icon: "✓", title: "100% Chính hãng", text: "Cam kết nguồn gốc xuất xứ rõ ràng, tem mác đầy đủ." },
  { icon: "🚚", title: "Giao hàng nhanh", text: "Giao toàn quốc 2–5 ngày, freeship đơn từ 499K." },
  { icon: "↩", title: "Đổi trả 7 ngày", text: "Hỗ trợ đổi size, đổi mẫu nếu sản phẩm lỗi." },
  { icon: "💳", title: "Thanh toán linh hoạt", text: "COD, VNPAY, Momo — an toàn & tiện lợi." },
];
</script>
