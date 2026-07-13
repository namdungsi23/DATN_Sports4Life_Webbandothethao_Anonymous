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
const cartLineKey = (item) => item.variantId ?? item.productId;

const addToCart = (product, quantity = 1) => {
  if (!product?.id) return;
  const lineKey = product.variantId ?? product.id;
  const existing = state.cartItems.find((it) => cartLineKey(it) === lineKey);
  if (existing) {
    existing.quantity += normalizeQty(quantity);
    return;
  }
  state.cartItems.push({
    productId: product.id,
    variantId: product.variantId ?? null,
    size: product.size ?? null,
    color: product.color ?? null,
    name: product.name || "Sản phẩm",
    price: Number(product.price || 0),
    quantity: normalizeQty(quantity),
    image: resolveProductImage(product),
  });
};

const removeFromCart = (lineKey) => {
  state.cartItems = state.cartItems.filter((item) => cartLineKey(item) !== lineKey);
};

const updateCartQuantity = (lineKey, quantity) => {
  const item = state.cartItems.find((it) => cartLineKey(it) === lineKey);
  if (!item) return;
  item.quantity = normalizeQty(quantity);
};

const clearCart = () => {
  state.cartItems = [];
};

const isLoggedIn = () => Boolean(state.user);

const isFavorite = (productId) =>
  Boolean(state.user) && state.favorites.some((item) => item.id === productId);

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
 * @returns {Promise<{ success: boolean, requiresLogin?: boolean, added?: boolean }>}
 */
const toggleFavorite = async (product) => {
  if (!state.user) {
    return { success: false, requiresLogin: true };
  }
  if (!product?.id) {
    return { success: false };
  }

  const index = state.favorites.findIndex((item) => item.id === product.id);
  const removing = index >= 0;

  try {
    if (removing) {
      await removeWishlistApi(product.id);
      state.favorites.splice(index, 1);
      return { success: true, added: false };
    }

    await addWishlistApi(product.id);
    state.favorites.unshift({
      id: product.id,
      name: product.name || "Sản phẩm",
      price: Number(product.price || product.minPrice || 0),
      image: resolveProductImage(product),
      categoryName: product.categoryName || "",
      categoryId: product.categoryId || "",
      inStock: product.inStock !== false,
      brand: product.brand || "",
    });
    return { success: true, added: true };
  } catch (err) {
    console.warn("Toggle favorite failed", err);
    return { success: false };
  }
};

const removeFavorite = async (productId) => {
  if (!state.user || !productId) return false;

  try {
    await removeWishlistApi(productId);
    state.favorites = state.favorites.filter((item) => item.id !== productId);
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
  cartLineKey,
});
