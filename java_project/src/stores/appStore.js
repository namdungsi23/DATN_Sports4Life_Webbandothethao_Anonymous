import { computed, reactive, watch } from "vue";
import { resolveProductImage } from "../utils/productImage";
import { addWishlistApi, fetchWishlistApi, removeWishlistApi } from "../services/api";

export const STORAGE_KEYS = {
  cart: "sport-store-cart",
  user: "sport-store-user",
  accessToken: "sport-store-access-token",
  refreshToken: "sport-store-refresh-token",
};

const safeParse = (raw, fallback) => {
  try {
    return raw ? JSON.parse(raw) : fallback;
  } catch {
    return fallback;
  }
};

const readStoredUser = () => safeParse(localStorage.getItem(STORAGE_KEYS.user), null);

const initialUser = readStoredUser();

const state = reactive({
  cartItems: safeParse(localStorage.getItem(STORAGE_KEYS.cart), []),
  user: initialUser,
  accessToken: sessionStorage.getItem(STORAGE_KEYS.accessToken) || null,
  favorites: [],
  favoritesLoading: false,
  toasts: [],
});

watch(
  () => state.cartItems,
  (value) => localStorage.setItem(STORAGE_KEYS.cart, JSON.stringify(value)),
  { deep: true }
);

watch(
  () => state.user,
  (value) => {
    if (value) {
      localStorage.setItem(STORAGE_KEYS.user, JSON.stringify(value));
      return;
    }
    localStorage.removeItem(STORAGE_KEYS.user);
  },
  { deep: true }
);

watch(
  () => state.accessToken,
  (value) => {
    if (value) {
      sessionStorage.setItem(STORAGE_KEYS.accessToken, value);
    } else {
      sessionStorage.removeItem(STORAGE_KEYS.accessToken);
    }
  }
);

const toFavoriteItem = (item) => {
  const product = item?.product || item;
  if (!product?.id) return null;
  return {
    id: product.id,
    wishlistId: item?.wishlistId,
    name: product.name || "Sản phẩm",
    price: Number(product.price ?? product.minPrice ?? 0),
    image: resolveProductImage(product),
    categoryName: product.categoryName || "",
    categoryId: product.categoryId || "",
    inStock: product.inStock !== false,
    brand: product.brand || "",
    addedAt: item?.createdAt,
  };
};

const applyWishlistItems = (items) => {
  state.favorites = (items || [])
    .map(toFavoriteItem)
    .filter(Boolean);
};

const normalizeQty = (qty) => {
  const parsed = Number(qty);
  if (!Number.isFinite(parsed) || parsed < 1) return 1;
  return Math.floor(parsed);
};

const resolveStock = (product) => {
  const raw = product?.stock ?? product?.quantity ?? product?.availableStock;
  if (raw == null || raw === "") return null;
  const n = Number(raw);
  return Number.isFinite(n) ? Math.max(0, Math.floor(n)) : null;
};

/**
 * @returns {{ success: boolean, reason?: string, added?: number, stock?: number }}
 */
const addToCart = (product, quantity = 1) => {
  if (!product?.id) return { success: false, reason: "invalid" };

  const stock = resolveStock(product);
  const requested = normalizeQty(quantity);
  const variantId = product.variantId ?? null;

  if (stock != null && stock <= 0) {
    return { success: false, reason: "out_of_stock", stock: 0 };
  }

  const existing = state.cartItems.find(
    (it) =>
      it.productId === product.id &&
      (it.variantId ?? null) === variantId
  );
  const currentQty = existing ? existing.quantity : 0;
  let target = currentQty + requested;

  if (stock != null && target > stock) {
    target = stock;
  }

  const added = target - currentQty;
  if (added <= 0) {
    return { success: false, reason: "stock_limit", stock, added: 0 };
  }

  if (existing) {
    existing.quantity = target;
    if (stock != null) existing.stock = stock;
    existing.price = Number(product.price || existing.price || 0);
    existing.size = product.size || existing.size || "";
    existing.color = product.color || existing.color || "";
    return { success: true, added, stock };
  }

  state.cartItems.push({
    productId: product.id,
    variantId,
    name: product.name || "Sản phẩm",
    price: Number(product.price || 0),
    quantity: target,
    stock: stock ?? undefined,
    image: resolveProductImage(product),
    size: product.size || "",
    color: product.color || "",
  });
  return { success: true, added, stock };
};

const removeFromCart = (productId, variantId = null) => {
  state.cartItems = state.cartItems.filter(
    (item) =>
      !(
        item.productId === productId &&
        (item.variantId ?? null) === (variantId ?? null)
      )
  );
};

/**
 * @returns {{ success: boolean, reason?: string, quantity: number, stock?: number, clamped?: boolean }}
 */
const updateCartQuantity = (productId, quantity, variantId = null) => {
  const item = state.cartItems.find(
    (it) =>
      it.productId === productId &&
      (it.variantId ?? null) === (variantId ?? null)
  );
  if (!item) return { success: false, reason: "not_found", quantity: 0 };

  const stock = resolveStock(item);
  let target = normalizeQty(quantity);
  let clamped = false;

  if (stock != null && target > stock) {
    target = Math.max(1, stock);
    clamped = true;
  }

  item.quantity = target;
  return { success: true, quantity: target, stock, clamped };
};

const clearCart = () => {
  state.cartItems = [];
};

const isLoggedIn = () => Boolean(state.user && state.accessToken);

const toProductId = (value) => {
  const n = Number(value);
  return Number.isFinite(n) ? n : null;
};

const isFavorite = (productId) => {
  const id = toProductId(productId);
  if (id == null || !state.user) return false;
  return state.favorites.some((item) => toProductId(item.id) === id);
};

const loadFavorites = async () => {
  if (!state.user) {
    state.favorites = [];
    return;
  }

  state.favoritesLoading = true;
  try {
    const data = await fetchWishlistApi();
    applyWishlistItems(data?.items || []);
  } catch (err) {
    console.warn("Load wishlist failed", err);
    state.favorites = [];
  } finally {
    state.favoritesLoading = false;
  }
};

/**
 * @returns {Promise<{ success: boolean, requiresLogin?: boolean, added?: boolean, message?: string }>}
 */
const toggleFavorite = async (product) => {
  if (!state.user) {
    return { success: false, requiresLogin: true, message: "Vui lòng đăng nhập để yêu thích sản phẩm." };
  }

  const productId = toProductId(product?.id);
  if (productId == null) {
    return { success: false, message: "Không xác định được sản phẩm." };
  }

  const index = state.favorites.findIndex((item) => toProductId(item.id) === productId);
  const removing = index >= 0;

  try {
    if (removing) {
      await removeWishlistApi(productId);
      state.favorites.splice(index, 1);
      return { success: true, added: false, message: "Đã bỏ khỏi yêu thích." };
    }

    await addWishlistApi(productId);
    state.favorites.unshift({
      id: productId,
      name: product.name || "Sản phẩm",
      price: Number(product.price || product.minPrice || 0),
      image: resolveProductImage(product),
      categoryName: product.categoryName || "",
      categoryId: product.categoryId || "",
      inStock: product.inStock !== false,
      brand: product.brand || "",
    });
    return { success: true, added: true, message: "Đã thêm vào yêu thích." };
  } catch (err) {
    console.warn("Toggle favorite failed", err);
    const apiMsg =
      err?.response?.data?.message ||
      err?.response?.data?.error ||
      err?.message;
    const status = err?.response?.status;
    let message = "Không thể cập nhật yêu thích. Vui lòng thử lại.";
    if (status === 401 || status === 403) {
      message = "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.";
    } else if (status === 404) {
      message = apiMsg ? String(apiMsg) : "Không tìm thấy sản phẩm hoặc hồ sơ người dùng.";
    } else if (apiMsg) {
      message = String(apiMsg).slice(0, 200);
    }
    return { success: false, message, requiresLogin: status === 401 };
  }
};

const removeFavorite = async (productId) => {
  const id = toProductId(productId);
  if (!state.user || id == null) return false;

  try {
    await removeWishlistApi(id);
    state.favorites = state.favorites.filter((item) => toProductId(item.id) !== id);
    return true;
  } catch (err) {
    console.warn("Remove favorite failed", err);
    return false;
  }
};

/**
 * @param {null | { username?: string, roles?: unknown, email?: string, accessToken?: string, refreshToken?: string }} payload
 */
const login = (payload) => {
  if (!payload) {
    state.user = null;
    state.accessToken = null;
    state.favorites = [];
    sessionStorage.removeItem(STORAGE_KEYS.refreshToken);
    sessionStorage.removeItem(STORAGE_KEYS.accessToken);
    return;
  }

  const { accessToken, refreshToken, permissions, panelAccess, canWriteCatalog, isAdmin, isStaff, ...profile } = payload;
  state.user =
    profile.username || profile.roles !== undefined || profile.email
      ? {
          ...profile,
          ...(Array.isArray(permissions) ? { permissions } : {}),
          ...(panelAccess !== undefined ? { panelAccess } : {}),
          ...(canWriteCatalog !== undefined ? { canWriteCatalog } : {}),
          ...(isAdmin !== undefined ? { isAdmin } : {}),
          ...(isStaff !== undefined ? { isStaff } : {}),
        }
      : null;
  state.accessToken = accessToken || null;
  state.favorites = [];

  if (accessToken) {
    try {
      sessionStorage.setItem(STORAGE_KEYS.accessToken, accessToken);
    } catch {
      /* ignore */
    }
  }

  if (refreshToken) {
    sessionStorage.setItem(STORAGE_KEYS.refreshToken, refreshToken);
  } else {
    sessionStorage.removeItem(STORAGE_KEYS.refreshToken);
  }
};

const logout = () => {
  clearCart();
  state.favorites = [];
  state.user = null;
  state.accessToken = null;
  sessionStorage.removeItem(STORAGE_KEYS.refreshToken);
  sessionStorage.removeItem(STORAGE_KEYS.accessToken);
  localStorage.removeItem(STORAGE_KEYS.user);
};

const updateUserProfile = (profile) => {
  if (!state.user || !profile) return;
  state.user = { ...state.user, ...profile };
};

const syncPanelAccess = (data) => {
  if (!state.user || !data) return;
  state.user = {
    ...state.user,
    roles: data.roles ?? state.user.roles,
    permissions: data.permissions ?? state.user.permissions,
    panelAccess: data.panelAccess ?? state.user.panelAccess,
    canWriteCatalog: data.canWriteCatalog ?? state.user.canWriteCatalog,
    isAdmin: data.isAdmin ?? state.user.isAdmin,
    isStaff: data.isStaff ?? state.user.isStaff,
  };
};

const getRefreshToken = () => sessionStorage.getItem(STORAGE_KEYS.refreshToken);

const cartCount = computed(() =>
  state.cartItems.reduce((sum, item) => sum + Number(item.quantity || 0), 0)
);

const cartAmount = computed(() =>
  state.cartItems.reduce(
    (sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 0),
    0
  )
);

const favoriteCount = computed(() => (state.user ? state.favorites.length : 0));

let toastSeq = 0;
const removeToast = (id) => {
  state.toasts = state.toasts.filter((t) => t.id !== id);
};

const pushToast = (message, type = "info", duration = 3500) => {
  const id = ++toastSeq;
  state.toasts.push({ id, message: String(message || ""), type });
  if (duration > 0) {
    window.setTimeout(() => removeToast(id), duration);
  }
  return id;
};

const toast = {
  success: (message, duration) => pushToast(message, "success", duration),
  error: (message, duration) => pushToast(message, "error", duration ?? 4500),
  warning: (message, duration) => pushToast(message, "warning", duration),
  info: (message, duration) => pushToast(message, "info", duration),
};

export const useAppStore = () => ({
  state,
  cartCount,
  cartAmount,
  favoriteCount,
  isLoggedIn,
  addToCart,
  removeFromCart,
  updateCartQuantity,
  clearCart,
  isFavorite,
  loadFavorites,
  toggleFavorite,
  removeFavorite,
  login,
  logout,
  updateUserProfile,
  syncPanelAccess,
  getRefreshToken,
  pushToast,
  removeToast,
  toast,
});

export const useToast = () => toast;
