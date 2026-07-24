<template>
  <img
    :src="currentSrc"
    :alt="alt"
    :loading="loading"
    :width="width"
    :height="height"
    :class="imgClass"
    @error="onError"
  />
</template>

<script setup>
import { ref, watch } from "vue";
import { FALLBACK_PRODUCT_IMAGE, resolveProductImage } from "../utils/productImage";

const props = defineProps({
  product: { type: Object, default: null },
  src: { type: String, default: "" },
  alt: { type: String, default: "" },
  loading: { type: String, default: "lazy" },
  width: { type: [String, Number], default: undefined },
  height: { type: [String, Number], default: undefined },
  imgClass: { type: String, default: "" },
});

const currentSrc = ref(FALLBACK_PRODUCT_IMAGE);

const updateSrc = () => {
  currentSrc.value = props.src || resolveProductImage(props.product);
};

watch(
  () => [props.src, props.product],
  updateSrc,
  { immediate: true, deep: true }
);

const onError = () => {
  if (currentSrc.value !== FALLBACK_PRODUCT_IMAGE) {
    currentSrc.value = FALLBACK_PRODUCT_IMAGE;
  }
};
</script>
