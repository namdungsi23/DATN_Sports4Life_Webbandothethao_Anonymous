<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useRoute } from "vue-router";
import { useAppStore } from "../stores/appStore";
import { userCanAccessPanel } from "../utils/adminAccess";

const route = useRoute();
const store = useAppStore();
const visible = ref(false);

const liftForChat = computed(() => {
  const path = route.path || "";
  if (!store.isLoggedIn()) return false;
  if (path.startsWith("/admin")) return false;
  if (path === "/login" || path === "/register") return false;
  if (userCanAccessPanel(store.state.user)) return false;
  return true;
});

const onScroll = () => {
  visible.value = window.scrollY > 280;
};

const scrollTop = () => {
  window.scrollTo({ top: 0, behavior: "smooth" });
};

onMounted(() => {
  onScroll();
  window.addEventListener("scroll", onScroll, { passive: true });
});

onUnmounted(() => {
  window.removeEventListener("scroll", onScroll);
});
</script>

<template>
  <Transition name="scroll-top">
    <button
      v-if="visible"
      type="button"
      class="scroll-top-btn"
      :class="{ 'scroll-top-btn--lift': liftForChat }"
      aria-label="Lướt lên đầu trang"
      title="Lướt lên đầu trang"
      @click="scrollTop"
    >
      <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
        <path d="M12 19V5M5 12l7-7 7 7" stroke-linecap="round" stroke-linejoin="round" />
      </svg>
    </button>
  </Transition>
</template>

<style scoped>
.scroll-top-btn {
  position: fixed;
  right: 22px;
  bottom: 24px;
  z-index: 1190;
  width: 48px;
  height: 48px;
  border: none;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, #1a3c34, #2d5a4e);
  color: #fff;
  cursor: pointer;
  box-shadow: 0 10px 24px rgba(26, 60, 52, 0.28);
  transition: transform 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.scroll-top-btn--lift {
  bottom: 88px;
}

.scroll-top-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 14px 28px rgba(26, 60, 52, 0.36);
  background: linear-gradient(145deg, #144038, #2a5448);
}

.scroll-top-enter-active,
.scroll-top-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.scroll-top-enter-from,
.scroll-top-leave-to {
  opacity: 0;
  transform: translateY(12px);
}

@media (max-width: 576px) {
  .scroll-top-btn {
    right: 16px;
    bottom: 18px;
    width: 44px;
    height: 44px;
  }

  .scroll-top-btn--lift {
    bottom: 80px;
  }
}
</style>
