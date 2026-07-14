<template>
  <AdminLayout>
    <div class="admin-chat">
      <div class="admin-chat__toolbar">
        <div class="admin-chat__filters">
          <button
            v-for="opt in statusFilters"
            :key="opt.value || 'all'"
            type="button"
            class="admin-chat__filter"
            :class="{ 'is-active': statusFilter === opt.value }"
            @click="changeFilter(opt.value)"
          >
            {{ opt.label }}
          </button>
        </div>
        <div class="admin-chat__meta">
          <span class="admin-chat__online" :class="{ 'is-on': presenceOnline }">
            {{ presenceOnline ? "Đang online (nhận hội thoại mới)" : "Đang kết nối…" }}
          </span>
          <button type="button" class="btn btn-sm btn-outline-secondary" :disabled="loadingList" @click="loadList">
            Làm mới
          </button>
        </div>
      </div>

      <div v-if="listError" class="alert alert-danger py-2">{{ listError }}</div>

      <div class="admin-chat__grid">
        <aside class="admin-chat__list admin-panel">
          <div class="admin-panel__head">
            <h2>Hội thoại ({{ conversations.length }})</h2>
          </div>
          <div class="admin-panel__body admin-chat__list-body">
            <div v-if="loadingList" class="admin-chat__empty">Đang tải…</div>
            <div v-else-if="!conversations.length" class="admin-chat__empty">
              Chưa có hội thoại được gán cho bạn.
            </div>
            <button
              v-for="item in conversations"
              :key="item.id"
              type="button"
              class="admin-chat__item"
              :class="{ 'is-active': item.id === activeId }"
              @click="selectConversation(item.id)"
            >
              <strong>{{ item.userFullName || `Khách #${item.userId}` }}</strong>
              <span class="admin-chat__item-status">{{ statusLabel(item.status) }}</span>
              <small>{{ formatDate(item.updatedAt || item.createdAt) }}</small>
            </button>
          </div>
        </aside>

        <section class="admin-chat__thread admin-panel">
          <div class="admin-panel__head admin-chat__thread-head">
            <div>
              <h2>{{ activeTitle }}</h2>
              <p v-if="activeDetail" class="admin-chat__thread-sub">
                {{ statusLabel(activeDetail.status) }}
                <template v-if="peerTyping"> · Khách đang nhập…</template>
              </p>
            </div>
            <div v-if="activeDetail" class="admin-chat__actions">
              <button
                v-if="activeDetail.status === 'OPEN'"
                type="button"
                class="btn btn-sm btn-outline-warning"
                @click="setStatus('RESOLVED')"
              >
                Đã xử lý
              </button>
              <button
                v-if="activeDetail.status === 'OPEN'"
                type="button"
                class="btn btn-sm btn-outline-secondary"
                @click="setStatus('CLOSED')"
              >
                Đóng
              </button>
            </div>
          </div>

          <div ref="threadEl" class="admin-panel__body admin-chat__messages">
            <div v-if="!activeId" class="admin-chat__empty">Chọn một hội thoại để trả lời.</div>
            <div v-else-if="detailLoading" class="admin-chat__empty">Đang tải tin nhắn…</div>
            <div v-else-if="detailError" class="admin-chat__empty text-danger">{{ detailError }}</div>
            <template v-else>
              <div
                v-for="msg in messages"
                :key="msg.id || `${msg.createdAt}-${msg.content}`"
                class="admin-chat__bubble"
                :class="msg.senderType === 'EMPLOYEE' ? 'is-mine' : 'is-theirs'"
              >
                <p>{{ msg.content }}</p>
                <time>{{ formatTime(msg.createdAt) }}</time>
              </div>
            </template>
          </div>

          <form v-if="activeId && activeDetail?.status === 'OPEN'" class="admin-chat__composer" @submit.prevent="onSend">
            <input
              v-model="draft"
              type="text"
              class="form-control"
              maxlength="4000"
              placeholder="Nhập phản hồi…"
              :disabled="sending"
              @input="onDraftInput"
            />
            <button type="submit" class="btn btn-warning text-white" :disabled="!canSend">Gửi</button>
          </form>
        </section>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import AdminLayout from "../../layouts/AdminLayout.vue";
import {
  fetchAdminChatConversationsApi,
  fetchAdminChatDetailApi,
  sendAdminChatMessageApi,
  updateAdminChatStatusApi,
} from "../../services/chatApi";
import { createChatSocket } from "../../services/chatSocket";

const statusFilters = [
  { value: "OPEN", label: "Đang mở" },
  { value: "", label: "Tất cả" },
  { value: "RESOLVED", label: "Đã xử lý" },
  { value: "CLOSED", label: "Đã đóng" },
];

const statusFilter = ref("OPEN");
const conversations = ref([]);
const loadingList = ref(false);
const listError = ref("");

const activeId = ref(null);
const activeDetail = ref(null);
const messages = ref([]);
const detailLoading = ref(false);
const detailError = ref("");
const draft = ref("");
const sending = ref(false);
const peerTyping = ref(false);
const presenceOnline = ref(false);
const threadEl = ref(null);

const socket = createChatSocket();
let typingTimer = null;
let peerTypingTimer = null;

const canSend = computed(
  () => Boolean(activeId.value) && draft.value.trim().length > 0 && !sending.value
);

const activeTitle = computed(() => {
  if (!activeDetail.value) return "Chi tiết hội thoại";
  return activeDetail.value.userFullName || `Khách #${activeDetail.value.userId}`;
});

socket.setHandlers({
  onConnect: () => {
    presenceOnline.value = true;
  },
  onDisconnect: () => {
    presenceOnline.value = false;
  },
  onMessage: (msg) => {
    if (!msg || msg.conversationId !== activeId.value) {
      loadList();
      return;
    }
    upsertMessage(msg);
    scrollThread();
    loadList();
  },
  onTyping: (payload) => {
    if (!payload?.typing) {
      peerTyping.value = false;
      return;
    }
    peerTyping.value = true;
    clearTimeout(peerTypingTimer);
    peerTypingTimer = setTimeout(() => {
      peerTyping.value = false;
    }, 2500);
  },
});

function statusLabel(status) {
  switch (status) {
    case "OPEN":
      return "Đang mở";
    case "RESOLVED":
      return "Đã xử lý";
    case "CLOSED":
      return "Đã đóng";
    default:
      return status || "";
  }
}

function formatDate(value) {
  if (!value) return "";
  try {
    return new Date(value).toLocaleString("vi-VN");
  } catch {
    return "";
  }
}

function formatTime(value) {
  if (!value) return "";
  try {
    return new Date(value).toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });
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

async function scrollThread() {
  await nextTick();
  const el = threadEl.value;
  if (el) el.scrollTop = el.scrollHeight;
}

async function loadList() {
  loadingList.value = true;
  listError.value = "";
  try {
    conversations.value = await fetchAdminChatConversationsApi(statusFilter.value || undefined);
  } catch (e) {
    listError.value = e?.message || "Không tải được danh sách hội thoại";
  } finally {
    loadingList.value = false;
  }
}

function changeFilter(value) {
  statusFilter.value = value;
  loadList();
}

async function selectConversation(id) {
  activeId.value = id;
  detailLoading.value = true;
  detailError.value = "";
  peerTyping.value = false;
  try {
    const detail = await fetchAdminChatDetailApi(id);
    activeDetail.value = detail;
    messages.value = Array.isArray(detail.messages) ? detail.messages : [];
    socket.subscribeConversation(id);
    await scrollThread();
  } catch (e) {
    detailError.value = e?.message || "Không tải được hội thoại";
    activeDetail.value = null;
    messages.value = [];
  } finally {
    detailLoading.value = false;
  }
}

async function onSend() {
  const content = draft.value.trim();
  if (!canSend.value) return;
  sending.value = true;
  try {
    if (socket.isConnected()) {
      socket.sendMessage(activeId.value, content);
      socket.sendTyping(activeId.value, false);
    } else {
      const saved = await sendAdminChatMessageApi(activeId.value, content);
      upsertMessage(saved);
      await scrollThread();
    }
    draft.value = "";
    loadList();
  } catch (e) {
    detailError.value = e?.message || "Gửi tin thất bại";
  } finally {
    sending.value = false;
  }
}

function onDraftInput() {
  if (!activeId.value || !socket.isConnected()) return;
  socket.sendTyping(activeId.value, true);
  clearTimeout(typingTimer);
  typingTimer = setTimeout(() => {
    socket.sendTyping(activeId.value, false);
  }, 1200);
}

async function setStatus(status) {
  if (!activeId.value) return;
  try {
    await updateAdminChatStatusApi(activeId.value, status);
    await selectConversation(activeId.value);
    await loadList();
  } catch (e) {
    detailError.value = e?.message || "Cập nhật trạng thái thất bại";
  }
}

onMounted(async () => {
  await loadList();
  try {
    await socket.connect();
  } catch {
    presenceOnline.value = false;
  }
});

onBeforeUnmount(() => {
  clearTimeout(typingTimer);
  clearTimeout(peerTypingTimer);
  socket.disconnect();
});
</script>

<style scoped>
.admin-chat__toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.admin-chat__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.admin-chat__filter {
  border: 1px solid #d7dde5;
  background: #fff;
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 0.85rem;
  cursor: pointer;
}

.admin-chat__filter.is-active {
  background: #1a3c34;
  border-color: #1a3c34;
  color: #fff;
}

.admin-chat__meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.admin-chat__online {
  font-size: 0.85rem;
  color: #8a96a3;
}

.admin-chat__online.is-on {
  color: #1a7f4b;
  font-weight: 600;
}

.admin-chat__grid {
  display: grid;
  grid-template-columns: minmax(240px, 320px) 1fr;
  gap: 14px;
  min-height: 560px;
}

.admin-chat__list-body {
  padding: 0 !important;
  max-height: 620px;
  overflow-y: auto;
}

.admin-chat__item {
  width: 100%;
  text-align: left;
  border: 0;
  border-bottom: 1px solid #edf1f5;
  background: #fff;
  padding: 12px 14px;
  display: grid;
  gap: 2px;
  cursor: pointer;
}

.admin-chat__item:hover,
.admin-chat__item.is-active {
  background: #f4faf7;
}

.admin-chat__item-status {
  font-size: 0.78rem;
  color: #e8871e;
}

.admin-chat__item small {
  color: #8a96a3;
}

.admin-chat__thread {
  display: flex;
  flex-direction: column;
  min-height: 560px;
}

.admin-chat__thread-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.admin-chat__thread-sub {
  margin: 4px 0 0;
  color: #6b7c76;
  font-size: 0.85rem;
}

.admin-chat__actions {
  display: flex;
  gap: 8px;
}

.admin-chat__messages {
  flex: 1;
  max-height: 480px;
  overflow-y: auto;
  background: #faf8f5;
}

.admin-chat__empty {
  text-align: center;
  color: #7a8793;
  padding: 36px 12px;
}

.admin-chat__bubble {
  max-width: 75%;
  margin-bottom: 10px;
  padding: 10px 12px;
  border-radius: 12px;
}

.admin-chat__bubble p {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.admin-chat__bubble time {
  display: block;
  margin-top: 4px;
  font-size: 0.72rem;
  opacity: 0.7;
}

.admin-chat__bubble.is-mine {
  margin-left: auto;
  background: #1a3c34;
  color: #fff;
}

.admin-chat__bubble.is-theirs {
  margin-right: auto;
  background: #fff;
  border: 1px solid #e6ebe8;
}

.admin-chat__composer {
  display: flex;
  gap: 8px;
  padding: 12px 16px 16px;
  border-top: 1px solid #edf1f5;
}

@media (max-width: 900px) {
  .admin-chat__grid {
    grid-template-columns: 1fr;
  }

  .admin-chat__list-body {
    max-height: 240px;
  }
}
</style>
