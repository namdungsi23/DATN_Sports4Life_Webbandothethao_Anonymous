<template>
  <div class="toast-stack" aria-live="polite" aria-atomic="true">
    <div
      v-for="item in toasts"
      :key="item.id"
      class="toast-item"
      :class="`toast-item--${item.type}`"
      role="status"
    >
      <span class="toast-item__msg">{{ item.message }}</span>
      <button type="button" class="toast-item__close" aria-label="Đóng" @click="removeToast(item.id)">
        ×
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useAppStore } from "../stores/appStore";

const store = useAppStore();
const toasts = computed(() => store.state.toasts);
const removeToast = (id) => store.removeToast(id);
</script>

<style scoped>
.toast-stack {
  position: fixed;
  top: 1rem;
  right: 1rem;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  max-width: min(360px, calc(100vw - 2rem));
  pointer-events: none;
}

.toast-item {
  pointer-events: auto;
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.75rem 0.9rem;
  border-radius: 0.5rem;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  background: #fff;
  border-left: 4px solid #6c757d;
  animation: toast-in 0.2s ease-out;
}

.toast-item--success {
  border-left-color: #198754;
}

.toast-item--error {
  border-left-color: #dc3545;
}

.toast-item--warning {
  border-left-color: #ffc107;
}

.toast-item--info {
  border-left-color: #0d6efd;
}

.toast-item__msg {
  flex: 1;
  font-size: 0.9rem;
  line-height: 1.4;
  color: #212529;
}

.toast-item__close {
  border: 0;
  background: transparent;
  font-size: 1.1rem;
  line-height: 1;
  color: #6c757d;
  cursor: pointer;
  padding: 0;
}

@keyframes toast-in {
  from {
    opacity: 0;
    transform: translateY(-6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
