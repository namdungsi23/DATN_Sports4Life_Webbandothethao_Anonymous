<template>
  <section class="brand-strip">
    <div class="section-head">
      <div>
        <p class="section-head__eyebrow">Đối tác chính thức</p>
        <h2 class="section-head__title">Thương hiệu nổi bật</h2>
      </div>
      <RouterLink v-if="showAllLink" to="/brands" class="section-head__link">Xem tất cả →</RouterLink>
    </div>

    <p v-if="loading" class="brand-strip__status">Đang tải thương hiệu...</p>

    <div
      v-else
      class="brand-strip-marquee"
      @mouseenter="paused = true"
      @mouseleave="paused = false"
    >
      <div ref="trackRef" class="brand-strip-marquee__track">
        <RouterLink
          v-for="(brand, index) in loopBrands"
          :key="`${brand.name}-${index}`"
          :to="brandLink(brand.name)"
          class="brand-logo-card"
          :title="brand.name"
        >
          <img
            v-if="brand.logo && !failedLogos.has(brand.name)"
            :src="brand.logo"
            :alt="`Logo ${brand.name}`"
            loading="lazy"
            @error="onLogoError(brand.name)"
          />
          <span v-else class="brand-logo-card__fallback">{{ brand.name }}</span>
        </RouterLink>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { RouterLink } from "vue-router";
import { fetchBrandsApi } from "../services/api";
import { DEFAULT_BRAND_NAMES, resolveBrandLogo } from "../utils/brandLogos";

defineProps({
  showAllLink: { type: Boolean, default: true },
});

const trackRef = ref(null);
const brands = ref([]);
const loading = ref(true);
const paused = ref(false);
const failedLogos = ref(new Set());
let animationId = 0;
let scrollDirection = 1;

const toBrandItem = (name) => ({
  name: String(name).trim(),
  logo: resolveBrandLogo(name),
});

const onLogoError = (name) => {
  failedLogos.value.add(name);
};

const brandLink = (name) => ({
  path: "/product",
  query: { keyword: name },
});

const loopBrands = computed(() => {
  const base = brands.value.length
    ? brands.value
    : DEFAULT_BRAND_NAMES.map(toBrandItem);
  return [...base, ...base];
});

const loadBrands = async () => {
  loading.value = true;
  try {
    const data = await fetchBrandsApi();
    const fromApi = (data?.brands ?? [])
      .map((item) => toBrandItem(item.name))
      .filter((item) => item.name);

    brands.value = fromApi.length ? fromApi : DEFAULT_BRAND_NAMES.map(toBrandItem);
  } catch (error) {
    console.warn("Load brands failed", error);
    brands.value = DEFAULT_BRAND_NAMES.map(toBrandItem);
  } finally {
    loading.value = false;
  }
};

const tick = () => {
  const track = trackRef.value;
  if (track && !paused.value) {
    track.scrollLeft += scrollDirection * 0.9;

    const maxScroll = track.scrollWidth / 2;
    if (track.scrollLeft >= maxScroll) {
      scrollDirection = -1;
    } else if (track.scrollLeft <= 0) {
      scrollDirection = 1;
    }
  }

  animationId = window.requestAnimationFrame(tick);
};

onMounted(async () => {
  await loadBrands();
  animationId = window.requestAnimationFrame(tick);
});

onUnmounted(() => {
  window.cancelAnimationFrame(animationId);
});
</script>
