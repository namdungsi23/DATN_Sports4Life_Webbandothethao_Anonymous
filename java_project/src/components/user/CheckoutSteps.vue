<template>
  <div class="checkout-steps">
    <div
      v-for="(step, index) in steps"
      :key="step.id"
      class="checkout-steps__item"
      :class="{
        'checkout-steps__item--active': current === step.id,
        'checkout-steps__item--done': stepOrder(step.id) < stepOrder(current),
      }"
    >
      <div class="checkout-steps__circle">
        <span v-if="stepOrder(step.id) < stepOrder(current)">✓</span>
        <span v-else>{{ index + 1 }}</span>
      </div>
      <div class="checkout-steps__label">{{ step.label }}</div>
      <div v-if="index < steps.length - 1" class="checkout-steps__line" />
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  current: {
    type: String,
    default: "payment",
  },
});

const steps = [
  { id: "payment", label: "Thanh toán" },
  { id: "shipping", label: "Giao hàng" },
  { id: "confirm", label: "Xác nhận" },
];

const stepOrder = (id) => steps.findIndex((s) => s.id === id);
</script>
