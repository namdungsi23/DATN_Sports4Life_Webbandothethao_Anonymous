<template>

  <MainLayout>

    <template #full>

      <MarqueeTicker />

      <HeroBanner />

    </template>



    <BrandStrip />



    <BestSellerSection />



    <FeaturedProductSection />



    <div class="home-features">

      <div class="home-features__grid">

        <div v-for="item in features" :key="item.title" class="home-feature-card">

          <div class="home-feature-card__icon">{{ item.icon }}</div>

          <h5>{{ item.title }}</h5>

          <p>{{ item.text }}</p>

        </div>

      </div>

    </div>



    <div class="home-cta text-center">

      <h2 class="home-cta__title">Khám phá bộ sưu tập mới nhất</h2>

      <p class="home-cta__text">Giày thể thao chính hãng — phong cách hiện đại, giá tốt mỗi ngày</p>

      <RouterLink to="/product" class="btn home-cta__btn">Mua ngay</RouterLink>

    </div>

  </MainLayout>

</template>



<script setup>

import { RouterLink } from "vue-router";

import MainLayout from "../layouts/MainLayout.vue";

import HeroBanner from "../components/user/HeroBanner.vue";

import MarqueeTicker from "../components/user/MarqueeTicker.vue";

import BrandStrip from "../components/user/BrandStrip.vue";

import BestSellerSection from "../components/user/BestSellerSection.vue";

import FeaturedProductSection from "../components/user/FeaturedProductSection.vue";



const features = [

  { icon: "✓", title: "Chính hãng", text: "Cam kết sản phẩm 100% chính hãng." },

  { icon: "🚚", title: "Giao hàng nhanh", text: "Giao hàng toàn quốc nhanh chóng." },

  { icon: "↩", title: "Hỗ trợ đổi trả", text: "Hỗ trợ đổi trả nếu sản phẩm lỗi." },

];

</script>


