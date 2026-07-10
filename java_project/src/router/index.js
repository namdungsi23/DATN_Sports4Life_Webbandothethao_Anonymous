import { createRouter, createWebHistory } from "vue-router";
import IndexView from "../pages/IndexView.vue";
import ProductView from "../pages/ProductView.vue";
import ProductDetailView from "../pages/ProductDetailView.vue";

import LoginView from "../pages/LoginView.vue";
import RegisterView from "../pages/RegisterView.vue";
import ProfileView from "../pages/ProfileView.vue";
import FavoritesView from "../pages/FavoritesView.vue";
import ContactView from "../pages/ContactView.vue";
import MoreView from "../pages/MoreView.vue";
import BrandsView from "../pages/BrandsView.vue";
import FeaturedView from "../pages/FeaturedView.vue";
import CartView from "../pages/cart/CartView.vue";
import CheckoutView from "../pages/cart/CheckoutView.vue";
import PaymentView from "../pages/cart/PaymentView.vue";
import AddressBookView from "../pages/AddressBookView.vue";
import AdminProductView from "../pages/admin/AdminProductView.vue";
import AdminCategoryView from "../pages/admin/AdminCategoryView.vue";
import AdminUserView from "../pages/admin/AdminUserView.vue";
import AdminOrderView from "../pages/admin/AdminOrderView.vue";
import AdminDashboardView from "../pages/admin/AdminDashboardView.vue";
import { STORAGE_KEYS } from "../stores/appStore";
import { ADMIN_PERMS } from "../utils/adminAccess.js";
import {
  resolveDefaultAdminRoute,
  userCanAccessPanel,
  userHasPermission,
} from "../utils/adminAccess.js";

const panelMeta = { requiresAuth: true, requiresPanel: true };

const routes = [
  { path: "/", component: IndexView },
  { path: "/product", component: ProductView },
  { path: "/product/:id", component: ProductDetailView },
  { path: "/brands", component: BrandsView },
  { path: "/featured", component: FeaturedView },
  { path: "/login", component: LoginView, meta: { guestOnly: true } },
  { path: "/register", component: RegisterView, meta: { guestOnly: true } },
  { path: "/profile", component: ProfileView, meta: { requiresAuth: true } },
  { path: "/addresses", component: AddressBookView, meta: { requiresAuth: true } },
  { path: "/favorites", component: FavoritesView, meta: { requiresAuth: true } },
  { path: "/contact", component: ContactView },
  { path: "/more", component: MoreView },
  { path: "/cart", component: CartView },
  { path: "/cart/checkout", component: CheckoutView, meta: { requiresAuth: true } },
  { path: "/cart/payment", component: PaymentView, meta: { requiresAuth: true } },
  { path: "/admin", redirect: "/admin/dashboard" },
  {
    path: "/admin/dashboard",
    component: AdminDashboardView,
    meta: { ...panelMeta, permission: ADMIN_PERMS.DASHBOARD, pageTitle: "Dashboard", pageSubtitle: "Tổng quan hệ thống" },
  },
  {
    path: "/admin/product",
    component: AdminProductView,
    meta: { ...panelMeta, permission: ADMIN_PERMS.PRODUCT, pageTitle: "Sản phẩm", pageSubtitle: "Quản lý sản phẩm" },
  },
  {
    path: "/admin/category",
    component: AdminCategoryView,
    meta: { ...panelMeta, permission: ADMIN_PERMS.CATEGORY, pageTitle: "Danh mục", pageSubtitle: "Quản lý danh mục" },
  },
  {
    path: "/admin/user",
    component: AdminUserView,
    meta: { ...panelMeta, permission: ADMIN_PERMS.USER, pageTitle: "Tài khoản", pageSubtitle: "Quản lý người dùng" },
  },
  {
    path: "/admin/order",
    component: AdminOrderView,
    meta: { ...panelMeta, permission: ADMIN_PERMS.ORDER, pageTitle: "Đơn hàng", pageSubtitle: "Xác nhận & quản lý đơn hàng" },
  },
  {
    path: "/admin/order/:id",
    component: AdminOrderView,
    meta: { ...panelMeta, permission: ADMIN_PERMS.ORDER, pageTitle: "Chi tiết đơn hàng", pageSubtitle: "Cập nhật trạng thái đơn hàng" },
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to) => {
  const rawUser = localStorage.getItem(STORAGE_KEYS.user);
  const user = rawUser ? JSON.parse(rawUser) : null;

  const needsUser = Boolean(to.meta.requiresAuth || to.meta.requiresPanel || to.meta.permission);

  if (needsUser && !user) return "/login";

  if (to.meta.requiresPanel && !userCanAccessPanel(user)) {
    return "/";
  }

  if (to.meta.permission && !userHasPermission(user, to.meta.permission)) {
    const fallback = resolveDefaultAdminRoute(user);
    return fallback && fallback !== to.path ? fallback : "/";
  }

  if (to.meta.guestOnly && user) return "/profile";
  return true;
});

export default router;