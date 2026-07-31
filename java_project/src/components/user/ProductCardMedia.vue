<template>
  <div class="pcm product-img">
    <div class="pcm__link" @click.prevent="onImageClick">
      <Transition name="pcm-fade" mode="out-in">
        <img
          :key="activeSrc"
          :src="activeSrc"
          :alt="product.name"
          class="pcm__img"
          loading="lazy"
          @error="onImgError"
        />
      </Transition>
    </div>

    <button
      type="button"
      class="pcm__quick"
      aria-label="Xem nhanh"
      title="Xem nhanh"
      @click.stop.prevent="emit('quick-view', product)"
    >
      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" stroke-linecap="round" stroke-linejoin="round" />
        <circle cx="12" cy="12" r="3" />
      </svg>
    </button>

    <slot />
  </div>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { FALLBACK_PRODUCT_IMAGE, resolveProductImage } from "../utils/productImage";

const props = defineProps({
  product: { type: Object, required: true },
  width: { type: [String, Number], default: undefined },
});

const emit = defineEmits(["quick-view"]);
const router = useRouter();

const activeIndex = ref(0);
const broken = ref(new Set());

const gallery = computed(() => {
  const list = Array.isArray(props.product?.gallery)
    ? props.product.gallery.filter(Boolean)
    : [];
  const unique = list.length
    ? [...new Set(list)].slice(0, 4)
    : [resolveProductImage(props.product)].filter(Boolean);
  return unique.filter((url) => !broken.value.has(url));
});

const activeSrc = computed(() => {
  const list = gallery.value;
  if (!list.length) return FALLBACK_PRODUCT_IMAGE;
  return list[activeIndex.value] || list[0] || FALLBACK_PRODUCT_IMAGE;
});

watch(
  () => props.product?.id,
  () => {
    activeIndex.value = 0;
    broken.value = new Set();
  }
);

const nextImage = () => {
  const len = gallery.value.length;
  if (len < 2) return;
  activeIndex.value = (activeIndex.value + 1) % len;
};

const onImageClick = () => {
  if (gallery.value.length > 1) {
    nextImage();
  } else {
    router.push(`/product/${props.product.id}`);
  }
};

const onImgError = () => {
  const src = activeSrc.value;
  if (src === FALLBACK_PRODUCT_IMAGE) return;
  const next = new Set(broken.value);
  next.add(src);
  broken.value = next;
  if (!gallery.value.length) {
    activeIndex.value = 0;
  } else if (activeIndex.value >= gallery.value.length) {
    activeIndex.value = 0;
  }
};
</script>

<style scoped>
.pcm {
  position: relative;
  overflow: hidden;
}

.pcm__link {
  display: block;
  width: 100%;
  height: 100%;
  position: relative;
  z-index: 1;
  cursor: pointer;
}

.pcm__img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* Fade khi đổi ảnh */
.pcm-fade-enter-active,
.pcm-fade-leave-active {
  transition: opacity 0.25s ease;
}

.pcm-fade-enter-from,
.pcm-fade-leave-to {
  opacity: 0;
}

/* Icon xem nhanh — hiện khi hover */
.pcm__quick {
  position: absolute;
  left: 50%;
  bottom: 10px;
  z-index: 8;
  transform: translateX(-50%);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 36px;
  padding: 0;
  border: none;
  border-radius: 8px;
  background: #fff;
  color: #374151;
  cursor: pointer;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.13);
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.2s ease, background 0.15s ease, color 0.15s ease;
}

.pcm__quick svg {
  display: block;
}

.pcm:hover .pcm__quick,
.pcm:focus-within .pcm__quick {
  opacity: 1;
  pointer-events: auto;
}

.pcm__quick:hover {
  background: #f3f4f6;
  color: #111;
}

@media (max-width: 640px) {
  .pcm__quick {
    opacity: 1;
    pointer-events: auto;
  }
}
</style>
