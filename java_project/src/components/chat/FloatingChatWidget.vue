<template>
  <div class="s4l-chat" :class="{ 's4l-chat--open': open }">
    <Transition name="s4l-chat-panel">
      <section v-if="open" class="s4l-chat__panel" role="dialog" aria-label="Chat hỗ trợ">
        <header class="s4l-chat__head">
          <div>
            <p class="s4l-chat__brand">Sports4Life</p>
            <h2 class="s4l-chat__title">Hỗ trợ trực tuyến</h2>
            <p class="s4l-chat__sub">
              {{ statusText }}
            </p>
          </div>
          <button type="button" class="s4l-chat__icon-btn" aria-label="Đóng chat" @click="close">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M6 6l12 12M18 6 6 18" />
            </svg>
          </button>
        </header>

        <div ref="listEl" class="s4l-chat__body">
          <div v-if="bootLoading" class="s4l-chat__state">Đang kết nối hỗ trợ…</div>
          <div v-else-if="error" class="s4l-chat__state s4l-chat__state--error">{{ error }}</div>
          <template v-else>
            <div v-if="!messages.length" class="s4l-chat__state">
              Xin chào! Hãy gửi câu hỏi về sản phẩm hoặc đơn hàng.
            </div>
            <div
              v-for="msg in messages"
              :key="msg.id || `${msg.createdAt}-${msg.content}`"
              class="s4l-chat__bubble"
              :class="isMine(msg) ? 's4l-chat__bubble--mine' : 's4l-chat__bubble--theirs'"
            >
              <p class="s4l-chat__bubble-text">{{ msg.content }}</p>
              <time class="s4l-chat__bubble-time">{{ formatTime(msg.createdAt) }}</time>
            </div>
            <p v-if="peerTyping" class="s4l-chat__typing">Nhân viên đang nhập…</p>
          </template>
        </div>

        <form class="s4l-chat__composer" @submit.prevent="onSend">
          <input
            v-model="draft"
            type="text"
            class="s4l-chat__input"
            maxlength="4000"
            placeholder="Nhập tin nhắn…"
            :disabled="!conversationId || sending || Boolean(error)"
            @input="onDraftInput"
          />
          <button
            type="submit"
            class="s4l-chat__send"
            :disabled="!canSend"
          >
            Gửi
          </button>
        </form>
      </section>
    </Transition>

    <button
      type="button"
      class="s4l-chat__fab"
      :aria-expanded="open"
      aria-label="Mở chat hỗ trợ"
      @click="toggle"
    >
      <span class="s4l-chat__fab-icon" aria-hidden="true">
        <svg v-if="!open" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8">
          <path d="M21 12a8.5 8.5 0 0 1-8.5 8.5H8l-4 2.2.8-3.4A8.5 8.5 0 1 1 21 12z" />
        </svg>
        <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M6 6l12 12M18 6 6 18" />
        </svg>
      </span>
      <span class="s4l-chat__fab-label">{{ open ? "Đóng" : "Chat" }}</span>
    </button>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from "vue";
import {
  fetchChatConversationDetailApi,
  openChatConversationApi,
  sendChatMessageApi,
} from "../../services/chatApi";
import { createChatSocket } from "../../services/chatSocket";

const open = ref(false);
const bootLoading = ref(false);
const sending = ref(false);
const error = ref("");
const conversationId = ref(null);
const employeeName = ref("");
const messages = ref([]);
const draft = ref("");
const peerTyping = ref(false);
const listEl = ref(null);
const socketConnected = ref(false);

const socket = createChatSocket();
let typingTimer = null;
let peerTypingTimer = null;

const canSend = computed(
  () =>
    Boolean(conversationId.value) &&
    draft.value.trim().length > 0 &&
    !sending.value &&
    !bootLoading.value &&
    !error.value
);

const statusText = computed(() => {
  if (bootLoading.value) return "Đang kết nối…";
  if (error.value) return "Tạm thời không khả dụng";
  if (employeeName.value) return `Phụ trách: ${employeeName.value}`;
  if (socketConnected.value) return "Đã kết nối realtime";
  return "Hỗ trợ khách hàng";
});

socket.setHandlers({
  onConnect: () => {
    socketConnected.value = true;
  },
  onDisconnect: () => {
    socketConnected.value = false;
  },
  onMessage: (msg) => {
    upsertMessage(msg);
    scrollToBottom();
  },
  onTyping: (payload) => {
    if (!payload || payload.typing === false) {
      peerTyping.value = false;
      return;
    }
    peerTyping.value = true;
    clearTimeout(peerTypingTimer);
    peerTypingTimer = setTimeout(() => {
      peerTyping.value = false;
    }, 2500);
  },
  onError: (msg) => {
    if (!messages.value.length) {
      error.value = typeof msg === "string" ? msg : "Lỗi kết nối chat";
    }
  },
});

function isMine(msg) {
  return msg?.senderType === "USER";
}

function formatTime(value) {
  if (!value) return "";
  try {
    return new Date(value).toLocaleTimeString("vi-VN", {
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return "";
  }
}

function upsertMessage(msg) {
  if (!msg) return;
  const idx = messages.value.findIndex((m) => m.id && msg.id && m.id === msg.id);
  if (idx >= 0) {
    messages.value[idx] = msg;
    return;
  }
  messages.value.push(msg);
}

async function scrollToBottom() {
  await nextTick();
  const el = listEl.value;
  if (el) el.scrollTop = el.scrollHeight;
}

async function bootstrap() {
  bootLoading.value = true;
  error.value = "";
  try {
    const created = await openChatConversationApi();
    conversationId.value = created.id;
    employeeName.value = created.employeeFullName || "";

    const detail = await fetchChatConversationDetailApi(created.id);
    messages.value = Array.isArray(detail.messages) ? detail.messages : [];
    employeeName.value = detail.employeeFullName || employeeName.value;

    try {
      await socket.connect();
      socket.subscribeConversation(created.id);
    } catch {
      /* REST vẫn dùng được nếu WS fail */
    }

    await scrollToBottom();
  } catch (e) {
    error.value = e?.message || "Không mở được chat hỗ trợ";
  } finally {
    bootLoading.value = false;
  }
}

async function onSend() {
  const content = draft.value.trim();
  if (!canSend.value) return;

  sending.value = true;
  try {
    if (socket.isConnected()) {
      socket.sendMessage(conversationId.value, content);
      socket.sendTyping(conversationId.value, false);
    } else {
      const saved = await sendChatMessageApi(conversationId.value, content);
      upsertMessage(saved);
      await scrollToBottom();
    }
    draft.value = "";
  } catch (e) {
    error.value = e?.message || "Gửi tin nhắn thất bại";
  } finally {
    sending.value = false;
  }
}

function onDraftInput() {
  if (!conversationId.value || !socket.isConnected()) return;
  socket.sendTyping(conversationId.value, true);
  clearTimeout(typingTimer);
  typingTimer = setTimeout(() => {
    socket.sendTyping(conversationId.value, false);
  }, 1200);
}

function toggle() {
  if (open.value) {
    close();
  } else {
    open.value = true;
    if (!conversationId.value && !bootLoading.value) {
      bootstrap();
    }
  }
}

function close() {
  open.value = false;
  peerTyping.value = false;
}

watch(open, (value) => {
  if (value) scrollToBottom();
});

onBeforeUnmount(() => {
  clearTimeout(typingTimer);
  clearTimeout(peerTypingTimer);
  socket.disconnect();
});
</script>
