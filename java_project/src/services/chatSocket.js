import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import { STORAGE_KEYS, useAppStore } from "../stores/appStore";

function getAccessToken() {
  const { state } = useAppStore();
  if (state.accessToken) return state.accessToken;
  try {
    return sessionStorage.getItem(STORAGE_KEYS.accessToken);
  } catch {
    return null;
  }
}

/**
 * STOMP over SockJS helper.
 * Endpoint: /base/ws (Vite proxy → Spring /ws)
 */
export function createChatSocket() {
  let client = null;
  let activeConversationId = null;
  let messageSub = null;
  let typingSub = null;

  const handlers = {
    onMessage: null,
    onTyping: null,
    onConnect: null,
    onDisconnect: null,
    onError: null,
  };

  function isConnected() {
    return Boolean(client?.connected);
  }

  function connect() {
    const token = getAccessToken();
    if (!token) {
      handlers.onError?.("Chưa đăng nhập");
      return Promise.reject(new Error("Chưa đăng nhập"));
    }

    if (client?.connected) {
      return Promise.resolve();
    }

    return new Promise((resolve, reject) => {
      client = new Client({
        webSocketFactory: () => new SockJS(`/base/ws?access_token=${encodeURIComponent(token)}`),
        connectHeaders: {
          Authorization: `Bearer ${token}`,
          access_token: token,
        },
        reconnectDelay: 4000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        onConnect: () => {
          handlers.onConnect?.();
          if (activeConversationId != null) {
            subscribeConversation(activeConversationId);
          }
          resolve();
        },
        onDisconnect: () => {
          handlers.onDisconnect?.();
        },
        onStompError: (frame) => {
          const msg = frame?.headers?.message || frame?.body || "Lỗi WebSocket";
          handlers.onError?.(msg);
          reject(new Error(msg));
        },
        onWebSocketError: () => {
          handlers.onError?.("Không kết nối được máy chủ chat");
        },
      });

      client.activate();
    });
  }

  function disconnect() {
    unsubscribeConversation();
    activeConversationId = null;
    if (client) {
      client.deactivate();
      client = null;
    }
  }

  function unsubscribeConversation() {
    messageSub?.unsubscribe();
    typingSub?.unsubscribe();
    messageSub = null;
    typingSub = null;
  }

  function subscribeConversation(conversationId) {
    if (!conversationId) return;
    activeConversationId = conversationId;
    if (!client?.connected) return;

    unsubscribeConversation();

    messageSub = client.subscribe(`/topic/conversations/${conversationId}`, (frame) => {
      try {
        const body = JSON.parse(frame.body);
        handlers.onMessage?.(body);
      } catch {
        /* ignore malformed */
      }
    });

    typingSub = client.subscribe(`/topic/conversations/${conversationId}/typing`, (frame) => {
      try {
        const body = JSON.parse(frame.body);
        handlers.onTyping?.(body);
      } catch {
        /* ignore */
      }
    });
  }

  function sendMessage(conversationId, content) {
    if (!client?.connected) {
      throw new Error("Chưa kết nối realtime");
    }
    client.publish({
      destination: "/app/chat.send",
      body: JSON.stringify({ conversationId, content }),
    });
  }

  function sendTyping(conversationId, typing) {
    if (!client?.connected || !conversationId) return;
    client.publish({
      destination: "/app/chat.typing",
      body: JSON.stringify({ conversationId, typing: Boolean(typing) }),
    });
  }

  return {
    connect,
    disconnect,
    isConnected,
    subscribeConversation,
    sendMessage,
    sendTyping,
    setHandlers(next) {
      Object.assign(handlers, next || {});
    },
  };
}
