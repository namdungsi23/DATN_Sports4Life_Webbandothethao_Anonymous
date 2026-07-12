import axios from "axios";
import { STORAGE_KEYS, useAppStore } from "../stores/appStore";
import { normalizeProductsResponse, normalizeProductDetailResponse } from "../utils/productImage";

const clientApi = axios.create({
  baseURL: "/base/api/public",
  withCredentials: true,
});

const clientBase = axios.create({
  baseURL: "/base",
  withCredentials: true,
});

function bearerFromStore() {
  const { state } = useAppStore();
  if (state.accessToken) return state.accessToken;
  try {
    return sessionStorage.getItem(STORAGE_KEYS.accessToken);
  } catch {
    return null;
  }
}

clientApi.interceptors.request.use((config) => {
  const token = bearerFromStore();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

const clientAuthApi = axios.create({
  baseURL: "/base/api",
  withCredentials: true,
});

clientAuthApi.interceptors.request.use((config) => {
  const token = bearerFromStore();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

function attachUnauthorizedHandler(client) {
  client.interceptors.response.use(
    (res) => res,
    (error) => {
      const status = error?.response?.status;
      if (status === 401) {
        try {
          useAppStore().logout();
        } catch {
          /* ignore */
        }
      }
      return Promise.reject(error);
    }
  );
}

attachUnauthorizedHandler(clientApi);
attachUnauthorizedHandler(clientBase);
attachUnauthorizedHandler(clientAuthApi);

export const fetchProductsApi = async (params) => {
  const response = await clientApi.get("/products", { params });
  return normalizeProductsResponse(response.data);
};

export const fetchProductByIdApi = async (id) => {
  const response = await clientApi.get(`/products/${id}`);
  return normalizeProductDetailResponse(response.data);
};

export const fetchProductCommentsApi = async (productId) => {
  const response = await clientApi.get(`/products/${productId}/comments`);
  return response.data;
};

export const postProductCommentApi = async (payload) => {
  const response = await clientAuthApi.post("/comments", payload);
  return response.data;
};

export const fetchBrandsApi = async () => {
  const response = await clientApi.get("/brands");
  return response.data;
};

export const fetchCategoriesApi = async () => {
  const response = await clientApi.get("/categories");
  const data = response.data;
  if (Array.isArray(data)) return data;
  if (data && typeof data === "object" && Array.isArray(data.data)) return data.data;
  if (Array.isArray(data?.categories)) return data.categories;
  return [];
};

export const loginApi = async (payload) => {
  const formData = new URLSearchParams();
  formData.append("username", payload.username);
  formData.append("pwd", payload.password);
  formData.append("remember-me", payload.remember ? "true" : "false");
  const response = await clientBase.post("/login/validate", formData);
  return response.data;
};

export const registerApi = async (payload) => {
  const response = await clientApi.post("/register", payload);
  return response.data;
};

export const forgotPasswordApi = async (payload) => {
  const response = await clientApi.post("/forgot-password", payload);
  return response.data;
};

export const verifyOtpApi = async (payload) => {
  const response = await clientApi.post("/verify-otp", payload);
  return response.data;
};

export const resetPasswordApi = async (payload) => {
  const response = await clientApi.post("/reset-password", payload);
  return response.data;
};

export const fetchProfileApi = async () => {
  const response = await clientAuthApi.get("/profile");
  return response.data;
};

export const updateProfileApi = async (payload) => {
  const response = await clientAuthApi.put("/profile", payload);
  return response.data;
};

export const uploadProfileAvatarApi = async (file) => {
  const formData = new FormData();
  formData.append("file", file);
  const response = await clientAuthApi.post("/profile/avatar", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return response.data;
};

export const changePasswordApi = async (payload) => {
  const response = await clientAuthApi.post("/profile/change-password", payload);
  return response.data;
};

export const fetchAdminMeApi = async () => {
  const response = await clientAuthApi.get("/admin/me");
  return response.data;
};

export const fetchAdminSearchApi = async (q) => {
  const response = await clientAuthApi.get("/admin/search", { params: { q } });
  return response.data;
};

export const fetchAdminNotificationsApi = async (limit = 20) => {
  const response = await clientAuthApi.get("/admin/notifications", { params: { limit } });
  return response.data;
};

export const fetchAdminUnreadCountApi = async () => {
  const response = await clientAuthApi.get("/admin/notifications/unread-count");
  return response.data;
};

export const markAdminNotificationReadApi = async (id) => {
  const response = await clientAuthApi.patch(`/admin/notifications/${id}/read`);
  return response.data;
};

export const markAllAdminNotificationsReadApi = async () => {
  const response = await clientAuthApi.patch("/admin/notifications/read-all");
  return response.data;
};

export const fetchWishlistApi = async () => {
  const response = await clientAuthApi.get("/wishlist");
  return response.data;
};

export const addWishlistApi = async (productId) => {
  const response = await clientAuthApi.post(`/wishlist/${productId}`);
  return response.data;
};

export const removeWishlistApi = async (productId) => {
  const response = await clientAuthApi.delete(`/wishlist/${productId}`);
  return response.data;
};

export const sendContactApi = async (payload) => {
  const response = await clientApi.post("/contact/send", payload);
  return response.data;
};

export const submitCheckoutApi = async (payload) => {
  const response = await clientApi.post("/checkout/pay", payload);
  return response.data;
};

export const fetchAddressesApi = async () => {
  const response = await clientAuthApi.get("/addresses");
  return response.data;
};

export const createAddressApi = async (payload) => {
  const response = await clientAuthApi.post("/addresses", payload);
  return response.data;
};

export const updateAddressApi = async (id, payload) => {
  const response = await clientAuthApi.put(`/addresses/${id}`, payload);
  return response.data;
};

export const deleteAddressApi = async (id) => {
  const response = await clientAuthApi.delete(`/addresses/${id}`);
  return response.data;
};

export const setDefaultAddressApi = async (id) => {
  const response = await clientAuthApi.post(`/addresses/${id}/default`);
  return response.data;
};

export const confirmPaymentApi = async (payload) => {
  const response = await clientAuthApi.post("/checkout/confirm", payload);
  return response.data;
};

export const fetchMyOrdersApi = async () => {
  const response = await clientAuthApi.get("/orders");
  return response.data;
};

export const fetchMyOrderDetailApi = async (id) => {
  const response = await clientAuthApi.get(`/orders/${id}`);
  return response.data;
};