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

export const fetchProductsApi = async (params) => {
  const response = await clientApi.get("/products", { params });
  return normalizeProductsResponse(response.data);
};

export const fetchProductByIdApi = async (id) => {
  const response = await clientApi.get(`/products/${id}`);
  return normalizeProductDetailResponse(response.data);
};

export const fetchBrandsApi = async () => {
  const response = await clientApi.get("/brands");
  return response.data;
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

export const fetchAdminMeApi = async () => {
  const response = await clientAuthApi.get("/admin/me");
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

export const confirmPaymentApi = async (payload) => {
  const response = await clientApi.post("/checkout/confirm", payload);
  return response.data;
};