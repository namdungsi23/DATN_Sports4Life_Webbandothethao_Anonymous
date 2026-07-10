<script setup>
/**
 * Giữ WebSocket employee online khi đang ở khu vực /admin/*
 * để Conversation mới có thể được gán.
 */
import { onBeforeUnmount, onMounted, ref } from "vue";
import { createChatSocket } from "../../services/chatSocket";

const socket = createChatSocket();
const online = ref(false);

onMounted(async () => {
  try {
    await socket.connect();
    online.value = true;
  } catch {
    online.value = false;
  }
});

onBeforeUnmount(() => {
  socket.disconnect();
  online.value = false;
});
</script>

<template>
  <!-- presence-only: không render UI -->
  <span class="admin-chat-presence" :data-online="online" hidden />
</template>
