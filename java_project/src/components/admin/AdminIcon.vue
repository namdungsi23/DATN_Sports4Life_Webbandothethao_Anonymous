<template>
  <span
    class="admin-icon"
    :class="[`admin-icon--${name}`, toneClass, { 'admin-icon--filled': filled }]"
    :style="sizeStyle"
  >
    <!-- Dashboard -->
    <svg v-if="name === 'dashboard'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
      <rect x="3" y="3" width="7" height="7" rx="1.5" />
      <rect x="14" y="3" width="7" height="7" rx="1.5" />
      <rect x="3" y="14" width="7" height="7" rx="1.5" />
      <rect x="14" y="14" width="7" height="7" rx="1.5" />
    </svg>

    <!-- Product -->
    <svg v-else-if="name === 'product'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
      <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
      <path d="M3.3 7.3 12 12l8.7-4.7" />
      <path d="M12 22V12" />
    </svg>

    <!-- Category -->
    <svg v-else-if="name === 'category'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
      <path d="M4 6h16" />
      <path d="M4 12h10" />
      <path d="M4 18h14" />
      <circle cx="17" cy="12" r="2" fill="currentColor" stroke="none" />
    </svg>

    <!-- Order -->
    <svg v-else-if="name === 'order'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
      <path d="M9 5H5a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-4" />
      <path d="M9 5a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2" />
      <path d="M9 12h6" />
      <path d="M9 16h4" />
    </svg>

    <!-- User -->
    <svg v-else-if="name === 'user'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
      <circle cx="12" cy="8" r="4" />
      <path d="M5 20c0-3.866 3.134-7 7-7s7 3.134 7 7" />
    </svg>

    <!-- Chat -->
    <svg v-else-if="name === 'chat'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
      <path d="M21 12a8.5 8.5 0 0 1-8.5 8.5H8l-4 2.2.8-3.4A8.5 8.5 0 1 1 21 12z" />
    </svg>

    <!-- Store -->
    <svg v-else-if="name === 'store'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
      <path d="M3 9l9-6 9 6v11a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9z" />
      <path d="M9 21V12h6v9" />
    </svg>

    <!-- Logout -->
    <svg v-else-if="name === 'logout'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
      <path d="M9 6V4a1 1 0 0 1 1-1h9a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1h-9a1 1 0 0 1-1-1v-2" />
      <path d="M14 12H3m0 0 3.5-3.5M3 12l3.5 3.5" />
    </svg>

    <!-- Menu -->
    <svg v-else-if="name === 'menu'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
      <path d="M4 6h16" />
      <path d="M4 12h16" />
      <path d="M4 18h16" />
    </svg>

    <!-- Close -->
    <svg v-else-if="name === 'close'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
      <path d="M6 6l12 12M18 6 6 18" />
    </svg>

    <!-- Users -->
    <svg v-else-if="name === 'users'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
      <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
      <circle cx="9" cy="7" r="4" />
      <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
      <path d="M16 3.13a4 4 0 0 1 0 7.75" />
    </svg>

    <!-- Chart -->
    <svg v-else-if="name === 'chart'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
      <path d="M3 3v18h18" />
      <path d="M7 16l4-6 4 3 5-7" />
    </svg>

    <!-- Package -->
    <svg v-else-if="name === 'package'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
    </svg>

    <!-- Bell -->
    <svg v-else-if="name === 'comment'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
      <path d="M21 15a4 4 0 0 1-4 4H8l-5 3V7a4 4 0 0 1 4-4h10a4 4 0 0 1 4 4z" />
    </svg>
    <svg v-else-if="name === 'rank'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
      <circle cx="12" cy="8" r="4" />
      <path d="M8.5 13.5 7 21l5-2.5L17 21l-1.5-7.5" />
    </svg>
    <svg v-else-if="name === 'bell'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
      <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
      <path d="M13.73 21a2 2 0 0 1-3.46 0" />
    </svg>

    <!-- Search -->
    <svg v-else-if="name === 'search'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
      <circle cx="11" cy="11" r="7" />
      <path d="M20 20l-3.5-3.5" />
    </svg>

    <!-- Home -->
    <svg v-else-if="name === 'home'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
      <path d="M3 9l9-6 9 6v11a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9z" />
      <path d="M9 21V12h6v9" />
    </svg>

    <!-- Settings -->
    <svg v-else-if="name === 'settings'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
      <circle cx="12" cy="12" r="3" />
      <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42" />
    </svg>

    <!-- Fallback -->
    <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
      <circle cx="12" cy="12" r="9" />
    </svg>
  </span>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: 20 },
  tone: { type: String, default: "" },
  filled: { type: Boolean, default: false },
});

const sizeStyle = computed(() => {
  const px = typeof props.size === "number" ? `${props.size}px` : props.size;
  return { width: px, height: px };
});

const toneClass = computed(() => (props.tone ? `admin-icon--tone-${props.tone}` : ""));
</script>
