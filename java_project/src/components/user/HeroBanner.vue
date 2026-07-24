<template>
  <section
    class="hero-slider"
    @mouseenter="pauseAutoplay"
    @mouseleave="resumeAutoplay"
    @touchstart.passive="onTouchStart"
    @touchend.passive="onTouchEnd"
  >
    <div class="hero-slider__track" :style="{ transform: `translateX(-${current * 100}%)` }">
      <article v-for="(slide, index) in slides" :key="index" class="hero-slider__slide">
        <img :src="slide.image" :alt="slide.title" class="hero-slider__img" />
        <div class="hero-slider__overlay">
          <p class="hero-eyebrow">{{ slide.eyebrow }}</p>
          <h1 class="hero-title">{{ slide.title }}</h1>
          <p v-if="slide.promo" class="hero-slider__promo">{{ slide.promo }}</p>
          <span v-if="slide.promoDate" class="hero-slider__promo-date">{{ slide.promoDate }}</span>
          <RouterLink v-if="slide.link" :to="slide.link" class="hero-slider__cta">
            {{ slide.linkLabel || "Mua ngay" }}
          </RouterLink>
        </div>
      </article>
    </div>

    <button type="button" class="hero-slider__arrow hero-slider__arrow--prev" aria-label="Slide trước" @click="prev">
      ‹
    </button>
    <button type="button" class="hero-slider__arrow hero-slider__arrow--next" aria-label="Slide sau" @click="next">
      ›
    </button>

    <div class="hero-slider__dots">
      <button
        v-for="(_, index) in slides"
        :key="index"
        type="button"
        class="hero-slider__dot"
        :class="{ 'is-active': index === current }"
        :aria-label="`Slide ${index + 1}`"
        @click="goTo(index)"
      />
    </div>

    <div class="hero-slider__progress">
      <div class="hero-slider__progress-bar" :key="progressKey" :style="{ animationDuration: `${interval}ms` }" />
    </div>
  </section>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from "vue";
import { RouterLink } from "vue-router";

const props = defineProps({
  interval: { type: Number, default: 5000 },
  slides: {
    type: Array,
    default: () => [
      {
        image: "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=1400&q=80",
        eyebrow: "TUẦN LỄ THỂ THAO",
        title: "Mùa lễ hội",
        promo: "MUA 2 SẢN PHẨM TẶNG 20%",
        promoDate: "Từ 10 - 16/12",
        link: "/product",
        linkLabel: "Khám phá ngay",
      },
      {
        image: "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=1400&q=80",
        eyebrow: "NEW COLLECTION",
        title: "Nike Air Max",
        promo: "GIẢM ĐẾN 30% BỘ SƯU TẬP MỚI",
        promoDate: "Chỉ trong tháng này",
        link: { path: "/product", query: { keyword: "Nike" } },
        linkLabel: "Xem Nike",
      },
      {
        image: "https://images.unsplash.com/photo-1606107557195-0f29cb4c6adc?w=1400&q=80",
        eyebrow: "ADIDAS ORIGINALS",
        title: "Ultraboost",
        promo: "FREESHIP TOÀN QUỐC ĐƠN TỪ 499K",
        promoDate: "Áp dụng mọi sản phẩm Adidas",
        link: { path: "/product", query: { keyword: "Adidas" } },
        linkLabel: "Xem Adidas",
      },
      {
        image: "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=1400&q=80",
        eyebrow: "SPORTS4LIFE",
        title: "Chinh phục mọi cung đường",
        promo: "HÀNG NGÀN MẪU GIÀY CHÍNH HÃNG",
        promoDate: "Cam kết 100% authentic",
        link: "/brands",
        linkLabel: "Thương hiệu",
      },
    ],
  },
});

const current = ref(0);
const progressKey = ref(0);
let timer = null;
let touchStartX = 0;

const goTo = (index) => {
  current.value = (index + props.slides.length) % props.slides.length;
  progressKey.value += 1;
  resetAutoplay();
};

const next = () => goTo(current.value + 1);
const prev = () => goTo(current.value - 1);

const startAutoplay = () => {
  timer = setInterval(() => next(), props.interval);
};

const resetAutoplay = () => {
  clearInterval(timer);
  startAutoplay();
};

const pauseAutoplay = () => clearInterval(timer);
const resumeAutoplay = () => startAutoplay();

const onTouchStart = (e) => {
  touchStartX = e.changedTouches[0].clientX;
};

const onTouchEnd = (e) => {
  const diff = touchStartX - e.changedTouches[0].clientX;
  if (Math.abs(diff) < 50) return;
  if (diff > 0) next();
  else prev();
};

onMounted(startAutoplay);
onUnmounted(() => clearInterval(timer));
</script>
