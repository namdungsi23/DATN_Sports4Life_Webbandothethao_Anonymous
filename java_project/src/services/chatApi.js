import axios from "axios";
import { STORAGE_KEYS, useAppStore } from "../stores/appStore";

function bearerFromStore() {
  const { state } = useAppStore();
  if (state.accessToken) return state.accessToken;
  try {
    return sessionStorage.getItem(STORAGE_KEYS.accessToken);
  } catch {
    return null;
  }
}

const chatApi = axios.create({
  baseURL: "/base/api",
  withCredentials: true,
});

chatApi.interceptors.request.use((config) => {
  const token = bearerFromStore();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

function unwrapError(error) {
  const data = error?.response?.data;
  if (typeof data === "string" && data.trim()) return data;
  if (data?.message) return data.message;
  if (data?.error) return data.error;
  return error?.message || "Có lỗi xảy ra";
}

/** User: mở / tái sử dụng conversation OPEN */
export async function openChatConversationApi() {
  try {
    const { data } = await chatApi.post("/chat/conversations/open");
    return data;
  } catch (error) {
    throw new Error(unwrapError(error));
  }
}

export async function fetchCurrentChatConversationApi() {
  try {
    const { data } = await chatApi.get("/chat/conversations/current");
    return data;
  } catch (error) {
    throw new Error(unwrapError(error));
  }
}

export async function fetchChatConversationDetailApi(id) {
  try {
    const { data } = await chatApi.get(`/chat/conversations/${id}`);
    return data;
  } catch (error) {
    throw new Error(unwrapError(error));
  }
}

export async function fetchChatMessagesApi(id) {
  try {
    const { data } = await chatApi.get(`/chat/conversations/${id}/messages`);
    return data;
  } catch (error) {
    throw new Error(unwrapError(error));
  }
}

export async function sendChatMessageApi(conversationId, content) {
  try {
    const { data } = await chatApi.post("/chat/messages", { conversationId, content });
    return data;
  } catch (error) {
    throw new Error(unwrapError(error));
  }
}

export async function markChatSeenApi(id) {
  try {
    const { data } = await chatApi.post(`/chat/conversations/${id}/seen`);
    return data;
  } catch (error) {
    throw new Error(unwrapError(error));
  }
}

/** Admin / Staff inbox */
export async function fetchAdminChatConversationsApi(status) {
  try {
    const { data } = await chatApi.get("/admin/chat/conversations", {
      params: status ? { status } : undefined,
    });
    return data;
  } catch (error) {
    throw new Error(unwrapError(error));
  }
}

export async function fetchAdminChatDetailApi(id) {
  try {
    const { data } = await chatApi.get(`/admin/chat/conversations/${id}`);
    return data;
  } catch (error) {
    throw new Error(unwrapError(error));
  }
}

export async function sendAdminChatMessageApi(conversationId, content) {
  try {
    const { data } = await chatApi.post("/admin/chat/messages", { conversationId, content });
    return data;
  } catch (error) {
    throw new Error(unwrapError(error));
  }
}

export async function updateAdminChatStatusApi(id, status) {
  try {
    const { data } = await chatApi.put(`/admin/chat/conversations/${id}/status`, null, {
      params: { status },
    });
    return data;
  } catch (error) {
    throw new Error(unwrapError(error));
  }
}

export async function markAdminChatSeenApi(id) {
  try {
    const { data } = await chatApi.post(`/admin/chat/conversations/${id}/seen`);
    return data;
  } catch (error) {
    throw new Error(unwrapError(error));
  }
}

export async function fetchAdminChatOnlineCountApi() {
  try {
    const { data } = await chatApi.get("/admin/chat/online-count");
    return data;
  } catch (error) {
    throw new Error(unwrapError(error));
  }
}
