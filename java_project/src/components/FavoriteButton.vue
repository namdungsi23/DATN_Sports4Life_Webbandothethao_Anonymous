<template>
  <button
    type="button"
    class="favorite-btn"
    :class="[
      `favorite-btn--${variant}`,
      `favorite-btn--${size}`,
      { 'is-active': liked, 'is-animating': animating },
    ]"
    :aria-label="liked ? 'Bỏ yêu thích' : 'Thêm vào yêu thích'"
    :title="liked ? 'Bỏ yêu thích' : 'Yêu thích'"
    @click.stop.prevent="onToggle"
  >
    <span class="favorite-btn__icon" aria-hidden="true">
      <svg viewBox="0 0 24 24">
        <path
          class="favorite-btn__heart"
          :class="{ 'is-filled': liked }"
          d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
        />
      </svg>
    </span>
    <span v-if="variant === 'pill' || variant === 'stack'" class="favorite-btn__label">
      {{ liked ? "Đã thích" : "Yêu thích" }}
    </span>
  </button>
</template>

<script setup>
import { computed, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAppStore } from "../stores/appStore";

const props = defineProps({
  product: { type: Object, required: true },
  variant: { type: String, default: "icon" },
  size: { type: String, default: "md" },
});

const emit = defineEmits(["login-required"]);

const router = useRouter();
const route = useRoute();
const { isFavorite, toggleFavorite } = useAppStore();
const animating = ref(false);

const liked = computed(() => isFavorite(props.product?.id));

const onToggle = async () => {
  if (!props.product?.id) return;

  const result = await toggleFavorite(props.product);
  if (result.requiresLogin) {
    emit("login-required");
    router.push({
      path: "/login",
      query: { redirect: route.fullPath },
    });
    return;
  }

  if (!result.success) return;

  animating.value = true;
  window.setTimeout(() => {
    animating.value = false;
  }, 350);
};
</script>

<style scoped>
.favorite-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: none;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

.favorite-btn__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 0;
}

.favorite-btn__icon svg {
  width: 18px;
  height: 18px;
  display: block;
}

.favorite-btn__heart {
  fill: transparent;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linejoin: round;
  transition: fill 0.2s ease, stroke 0.2s ease;
}

.favorite-btn__heart.is-filled {
  fill: currentColor;
  stroke: currentColor;
}

/* Icon trên ảnh (home, related) */
.favorite-btn--icon {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 3;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.95);
  color: #9ca3af;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}

.favorite-btn--icon:hover {
  transform: scale(1.08);
  color: #dc2626;
}

.favorite-btn--icon.is-active {
  color: #dc2626;
  background: #fff;
}

.favorite-btn--icon.favorite-btn--sm {
  width: 30px;
  height: 30px;
}

.favorite-btn--icon.favorite-btn--sm .favorite-btn__icon svg {
  width: 15px;
  height: 15px;
}

.favorite-btn--icon.favorite-btn--lg {
  width: 42px;
  height: 42px;
}

.favorite-btn--icon.favorite-btn--lg .favorite-btn__icon svg {
  width: 22px;
  height: 22px;
}

/* Nút pill — trang danh sách sản phẩm */
.favorite-btn--pill {
  flex: 1;
  min-width: 0;
  padding: 9px 12px;
  border-radius: 8px;
  border: 1.5px solid #e5e7eb;
  background: #fff;
  color: #4b5563;
  font-size: 13px;
  font-weight: 600;
}

.favorite-btn--pill:hover {
  border-color: #dc2626;
  color: #dc2626;
  background: #fef2f2;
}

.favorite-btn--pill.is-active {
  border-color: #dc2626;
  background: #fef2f2;
  color: #dc2626;
}

.favorite-btn--pill .favorite-btn__icon svg {
  width: 16px;
  height: 16px;
}

/* Detail page — icon + chữ dọc */
.favorite-btn--stack {
  flex-direction: column;
  gap: 4px;
  min-width: 72px;
  padding: 8px 10px;
  border-radius: 10px;
  border: 1.5px solid #e5e7eb;
  background: #fff;
  color: #6b7280;
}

.favorite-btn--stack.is-active {
  border-color: #dc2626;
  background: #fef2f2;
  color: #dc2626;
}

.favorite-btn--stack .favorite-btn__label {
  font-size: 11px;
  font-weight: 600;
}

.favorite-btn.is-animating .favorite-btn__icon {
  animation: favorite-pop 0.35s ease;
}

@keyframes favorite-pop {
  0% {
    transform: scale(1);
  }
  40% {
    transform: scale(1.28);
  }
  100% {
    transform: scale(1);
  }
}
</style>
