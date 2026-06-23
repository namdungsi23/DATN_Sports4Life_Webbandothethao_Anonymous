import { STORAGE_KEYS, useAppStore } from "../stores/appStore.js";

/** Spring Boot origin (session cookies). Set VITE_API_BASE in .env when needed. */
export const API_BASE =
  typeof import.meta !== "undefined" && import.meta.env?.VITE_API_BASE
    ? import.meta.env.VITE_API_BASE
    : "http://localhost:8080";

function getAccessToken() {
  try {
    const fromStore = useAppStore().state?.accessToken;
    if (fromStore) return fromStore;
  } catch {
    /* store not ready */
  }
  try {
    return sessionStorage.getItem(STORAGE_KEYS.accessToken);
  } catch {
    return null;
  }
}

export async function apiFetch(path, opts = {}) {
  const url = path.startsWith("http") ? path : `${API_BASE}${path}`;
  const isForm = opts.body instanceof FormData;
  const headers = { ...(opts.headers || {}) };

  const token = getAccessToken();
  if (token && !headers.Authorization) {
    headers.Authorization = `Bearer ${token}`;
  }

  if (!isForm && opts.body != null && !headers["Content-Type"]) {
    headers["Content-Type"] = "application/json";
  }

  const res = await fetch(url, {
    credentials: "include",
    ...opts,
    headers,
  });

  if (res.status === 401 || res.status === 403) {
    throw Object.assign(new Error("Không có quyền truy cập admin"), {
      status: res.status,
    });
  }

  const ct = res.headers.get("content-type") || "";
  const data = ct.includes("application/json") ? await res.json() : await res.text();

  if (!res.ok) {
    const msg =
      typeof data === "object" && data !== null && data.message
        ? data.message
        : typeof data === "string"
          ? data
          : res.statusText;
    throw Object.assign(new Error(msg), { status: res.status, body: data });
  }
  
  return data;
}
