<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";

defineProps({
  /** site | admin */
  variant: { type: String, default: "site" },
});

const now = ref(new Date());
let timer = null;

const label = computed(() =>
  new Intl.DateTimeFormat("vi-VN", {
    weekday: "long",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  }).format(now.value)
);

onMounted(() => {
  timer = setInterval(() => {
    now.value = new Date();
  }, 1000);
});

onUnmounted(() => {
  if (timer) clearInterval(timer);
});
</script>

<template>
  <time
    class="live-clock"
    :class="`live-clock--${variant}`"
    :datetime="now.toISOString()"
    :title="label"
  >
    {{ label }}
  </time>
</template>

<style scoped>
.live-clock {
  display: inline-flex;
  align-items: center;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.01em;
}

.live-clock--admin {
  font-size: 12px;
  font-weight: 600;
  color: #374151;
  padding: 8px 12px;
  border-radius: 10px;
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
}

@media (max-width: 992px) {
  .live-clock--admin {
    display: none;
  }
}
</style>
