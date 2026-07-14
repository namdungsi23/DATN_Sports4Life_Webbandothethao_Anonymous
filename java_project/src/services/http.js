import { STORAGE_KEYS, useAppStore } from "../stores/appStore.js";

/** Spring Boot origin. In dev, use Vite proxy prefix `/base`. */
export const API_BASE =
  typeof import.meta !== "undefined" && import.meta.env?.VITE_API_BASE
    ? import.meta.env.VITE_API_BASE
    : typeof import.meta !== "undefined" && import.meta.env?.DEV
      ? "/base"
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

  if (res.status === 401) {
    try {
      useAppStore().logout();
    } catch {
      /* ignore */
    }
    throw Object.assign(new Error("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."), {
      status: 401,
    });
  }

  if (res.status === 403) {
    throw Object.assign(new Error("Không có quyền truy cập."), {
      status: 403,
    });
  }

  const ct = res.headers.get("Content-Type") || "";
  let data;
  if (ct.includes("application/json")) {
    const raw = await res.text();
    try {
      data = raw ? JSON.parse(raw) : null;
    } catch {
      throw Object.assign(
        new Error("Phản hồi API không hợp lệ (JSON lỗi). Kiểm tra backend đang chạy và endpoint trả về JSON đúng định dạng."),
        { status: res.status, body: raw.slice(0, 500) }
      );
    }
  } else {
    data = await res.text();
  }

  if (!res.ok) {
    const msg =
      typeof data === "object" && data !== null && data.message
        ? data.message
        : typeof data === "string"
          ? data
          : res.statusText;
    const fieldErrors =
      typeof data === "object" && data !== null && data.errors && typeof data.errors === "object"
        ? data.errors
        : {};
    throw Object.assign(new Error(msg || "Yêu cầu thất bại"), {
      status: res.status,
      body: data,
      errors: fieldErrors,
      response: { status: res.status, data },
    });
  }
  
  return data;
}
