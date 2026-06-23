<template>
  <div class="marquee-ticker" aria-label="Thông báo khuyến mãi">
    <div class="marquee-ticker__track">
      <div class="marquee-ticker__content">
        <span v-for="(item, i) in items" :key="`a-${i}`" class="marquee-ticker__item">
          <span class="marquee-ticker__dot">★</span>
          {{ item }}
        </span>
      </div>
      <div class="marquee-ticker__content" aria-hidden="true">
        <span v-for="(item, i) in items" :key="`b-${i}`" class="marquee-ticker__item">
          <span class="marquee-ticker__dot">★</span>
          {{ item }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  items: {
    type: Array,
    default: () => [
      "FREESHIP đơn từ 499K toàn quốc",
      "GIẢM 20% khi mua 2 sản phẩm bất kỳ",
      "ĐỔI TRẢ MIỄN PHÍ trong 7 ngày",
      "HOTLINE hỗ trợ 24/7: 1900 6750",
      "SẢN PHẨM CHÍNH HÃNG 100%",
      "THANH TOÁN COD — VNPAY — MOMO",
    ],
  },
});
</script>
