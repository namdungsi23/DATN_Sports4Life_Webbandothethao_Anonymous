<template>
  <MainLayout>
    <template #full>
      <div class="page-hero page-hero--brands">
        <img
          src="https://images.unsplash.com/photo-1460353581641-37baddab0fa2?w=1600&q=80"
          alt="Thương hiệu thể thao Sports4Life"
        />
        <div class="page-hero__content site-container">
          <p class="page-hero__brand">Sports4Life</p>
          <p class="page-hero__eyebrow">Official partners</p>
          <h1>Thương hiệu</h1>
          <p>Nike, Adidas, Puma, Hoka, Jordan và hơn 20 thương hiệu — chính hãng, sẵn sàng cho mọi trận đấu</p>
          <RouterLink to="/product" class="page-hero__cta">Xem sản phẩm</RouterLink>
        </div>
      </div>
    </template>

    <section class="brands-page">
      <header class="brands-page__intro">
        <h2>Chọn thương hiệu</h2>
        <p>Nhấn vào logo để lọc sản phẩm theo thương hiệu bạn yêu thích</p>
      </header>

      <p v-if="loading" class="brands-page__status">Đang tải danh sách thương hiệu...</p>

      <div v-else-if="brands.length" class="brands-grid">
        <RouterLink
          v-for="(brand, index) in brands"
          :key="brand.name"
          :to="brandLink(brand.name)"
          class="brands-tile"
          :style="{ '--delay': `${index * 40}ms` }"
        >
          <div class="brands-tile__logo">
            <img
              v-if="brand.logo && !failedLogos.has(brand.name)"
              :src="brand.logo"
              :alt="brand.name"
              loading="lazy"
              @error="onLogoError(brand.name)"
            />
            <span v-else class="brands-tile__fallback">{{ brand.name }}</span>
          </div>
          <div class="brands-tile__meta">
            <strong>{{ brand.name }}</strong>
            <span>{{ brand.productCount }} sản phẩm</span>
          </div>
        </RouterLink>
      </div>

      <p v-else class="brands-page__status">Chưa có thương hiệu trong hệ thống.</p>
    </section>
  </MainLayout>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import MainLayout from "../layouts/MainLayout.vue";
import { fetchBrandsApi } from "../services/api";
import { DEFAULT_BRAND_NAMES, mergeBrandNames, resolveBrandLogo } from "../utils/brandLogos";

const brands = ref([]);
const loading = ref(true);
const failedLogos = ref(new Set());

const brandLink = (name) => ({
  path: "/product",
  query: { keyword: name },
});

const toBrandItem = (name, productCount = 0) => ({
  name: String(name).trim(),
  logo: resolveBrandLogo(name),
  productCount,
});

const onLogoError = (name) => {
  const next = new Set(failedLogos.value);
  next.add(name);
  failedLogos.value = next;
};

const loadBrands = async () => {
  loading.value = true;
  try {
    const data = await fetchBrandsApi();
    const apiItems = data?.brands ?? [];
    const countByName = new Map(
      apiItems.map((item) => [
        String(item?.name || "").trim().toLowerCase(),
        Number(item?.productCount) || 0,
      ])
    );
    const names = mergeBrandNames(apiItems.map((item) => item?.name));
    brands.value = names.map((name) =>
      toBrandItem(name, countByName.get(name.toLowerCase()) || 0)
    );
  } catch (error) {
    console.warn("Load brands failed", error);
    brands.value = DEFAULT_BRAND_NAMES.map((name) => toBrandItem(name));
  } finally {
    loading.value = false;
  }
};

onMounted(loadBrands);
</script>
