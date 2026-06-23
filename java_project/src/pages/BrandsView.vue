<template>
  <MainLayout>
    <div class="brands-page">
      <header class="brands-page__head">
        <h1>Thương hiệu</h1>
        <p>Chọn thương hiệu để xem sản phẩm chính hãng</p>
      </header>

      <BrandStrip :show-all-link="false" />

      <p v-if="loading" class="brands-page__status">Đang tải danh sách thương hiệu...</p>

      <div v-else-if="brands.length" class="brands-simple-grid">
        <RouterLink
          v-for="brand in brands"
          :key="brand.name"
          :to="brandLink(brand.name)"
          class="brands-simple-card"
        >
          <div class="brands-simple-card__logo">
            <img
              v-if="brand.logo && !failedLogos.has(brand.name)"
              :src="brand.logo"
              :alt="brand.name"
              loading="lazy"
              @error="onLogoError(brand.name)"
            />
            <span v-else>{{ brand.name }}</span>
          </div>
          <div class="brands-simple-card__meta">
            <strong>{{ brand.name }}</strong>
            <span>{{ brand.productCount }} sản phẩm</span>
          </div>
        </RouterLink>
      </div>

      <p v-else class="brands-page__status">Chưa có thương hiệu trong hệ thống.</p>
    </div>
  </MainLayout>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import MainLayout from "../layouts/MainLayout.vue";
import BrandStrip from "../components/BrandStrip.vue";
import { fetchBrandsApi } from "../services/api";
import { DEFAULT_BRAND_NAMES, resolveBrandLogo } from "../utils/brandLogos";

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
  failedLogos.value.add(name);
};

const loadBrands = async () => {
  loading.value = true;
  try {
    const data = await fetchBrandsApi();
    const fromApi = (data?.brands ?? [])
      .map((item) => toBrandItem(item.name, item.productCount ?? 0))
      .filter((item) => item.name);

    brands.value = fromApi.length
      ? fromApi
      : DEFAULT_BRAND_NAMES.map((name) => toBrandItem(name));
  } catch (error) {
    console.warn("Load brands failed", error);
    brands.value = DEFAULT_BRAND_NAMES.map((name) => toBrandItem(name));
  } finally {
    loading.value = false;
  }
};

onMounted(loadBrands);
</script>
