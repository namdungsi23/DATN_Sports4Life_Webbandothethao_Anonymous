<template>
  <div>
    <div class="product-grid">
      <article v-for="p in products.content" :key="p.id" class="product-item">
        <div class="product-img">
          <RouterLink :to="`/product/${p.id}`" class="product-item__link">
            <ProductImage :product="p" width="150" :alt="p.name" />
          </RouterLink>
        </div>

        <RouterLink :to="`/product/${p.id}`" class="product-item__name">
          <h4>{{ p.name }}</h4>
        </RouterLink>
        <p class="product-item__price">{{ formatPrice(p.price) }}đ</p>

        <div class="product-item__actions">
          <FavoriteButton :product="p" variant="pill" />
          <button
            v-if="p.inStock"
            type="button"
            class="product-item__cart-btn"
            @click="$emit('add-to-cart', p.id)"
          >
            Thêm vào giỏ
          </button>
          <span v-else class="product-item__soldout">Hết hàng</span>
        </div>
      </article>
    </div>

    <div class="product-sort-bar">
      <div class="product-sort-bar__label">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M3 6h18M7 12h10M10 18h4" stroke-linecap="round" />
        </svg>
        Sắp xếp theo
      </div>
      <div class="product-sort-bar__options">
        <button
          type="button"
          class="product-sort-bar__btn"
          :class="{ active: sort === 'createDate' && dir === 'desc' }"
          @click="$emit('sort-change', 'createDate', 'desc')"
        >
          Mới nhất
        </button>
        <button
          type="button"
          class="product-sort-bar__btn"
          :class="{ active: sort === 'price' && dir === 'asc' }"
          @click="$emit('sort-change', 'price', 'asc')"
        >
          Giá thấp → cao
        </button>
        <button
          type="button"
          class="product-sort-bar__btn"
          :class="{ active: sort === 'price' && dir === 'desc' }"
          @click="$emit('sort-change', 'price', 'desc')"
        >
          Giá cao → thấp
        </button>
        <button
          type="button"
          class="product-sort-bar__btn"
          :class="{ active: sort === 'name' && dir === 'asc' }"
          @click="$emit('sort-change', 'name', 'asc')"
        >
          Tên A → Z
        </button>
      </div>
    </div>

    <nav
      v-if="totalPages > 1"
      class="product-pagination"
      aria-label="Phân trang sản phẩm"
    >
      <p class="product-pagination__info">
        Trang <strong>{{ currentPage + 1 }}</strong> / {{ totalPages }}
      </p>

      <div class="product-pagination__controls">
        <button
          type="button"
          class="product-pagination__btn product-pagination__btn--nav"
          :disabled="currentPage === 0"
          aria-label="Trang đầu"
          @click="$emit('change-page', 0)"
        >
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M11 17l-5-5 5-5M18 17l-5-5 5-5" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </button>

        <button
          type="button"
          class="product-pagination__btn product-pagination__btn--nav"
          :disabled="currentPage === 0"
          aria-label="Trang trước"
          @click="$emit('change-page', currentPage - 1)"
        >
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M15 18l-6-6 6-6" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </button>

        <template v-for="item in pageItems" :key="item.key">
          <span v-if="item.type === 'ellipsis'" class="product-pagination__ellipsis">…</span>
          <button
            v-else
            type="button"
            class="product-pagination__btn product-pagination__btn--page"
            :class="{ active: item.value === currentPage }"
            :aria-label="`Trang ${item.value + 1}`"
            :aria-current="item.value === currentPage ? 'page' : undefined"
            @click="$emit('change-page', item.value)"
          >
            {{ item.value + 1 }}
          </button>
        </template>

        <button
          type="button"
          class="product-pagination__btn product-pagination__btn--nav"
          :disabled="currentPage >= totalPages - 1"
          aria-label="Trang sau"
          @click="$emit('change-page', currentPage + 1)"
        >
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 18l6-6-6-6" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </button>

        <button
          type="button"
          class="product-pagination__btn product-pagination__btn--nav"
          :disabled="currentPage >= totalPages - 1"
          aria-label="Trang cuối"
          @click="$emit('change-page', totalPages - 1)"
        >
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M6 17l5-5-5-5M13 17l5-5-5-5" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </button>
      </div>
    </nav>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { RouterLink } from "vue-router";
import FavoriteButton from "./FavoriteButton.vue";
import ProductImage from "./ProductImage.vue";

const props = defineProps({
  products: {
    type: Object,
    default: () => ({ content: [], totalPages: 0, number: 0 }),
  },
  sort: { type: String, default: "createDate" },
  dir: { type: String, default: "desc" },
});

defineEmits(["add-to-cart", "change-page", "sort-change"]);

const currentPage = computed(() => Number(props.products.number ?? 0));
const totalPages = computed(() => Number(props.products.totalPages ?? 0));

const buildVisiblePages = (current, total, siblingCount = 1) => {
  if (total <= 1) return [];

  const pages = new Set([0, total - 1]);
  for (let i = current - siblingCount; i <= current + siblingCount; i += 1) {
    if (i >= 0 && i < total) pages.add(i);
  }

  const sorted = [...pages].sort((a, b) => a - b);
  const items = [];

  sorted.forEach((page, index) => {
    if (index > 0 && page - sorted[index - 1] > 1) {
      items.push({ type: "ellipsis", key: `ellipsis-${sorted[index - 1]}-${page}` });
    }
    items.push({ type: "page", value: page, key: `page-${page}` });
  });

  return items;
};

const pageItems = computed(() => buildVisiblePages(currentPage.value, totalPages.value));

const formatPrice = (price) => Number(price || 0).toLocaleString("vi-VN");
</script>
