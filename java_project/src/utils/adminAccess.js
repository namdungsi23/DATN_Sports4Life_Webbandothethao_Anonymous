import { ROLE_ADMIN, ROLE_STAFF, userHasAnyRole, userIsAdmin } from "./roles";

/** Admin panel permission keys — align with PermissionName in Sport4L DB */
export const ADMIN_PERMS = {
  DASHBOARD: "DASHBOARD_VIEW",
  PRODUCT: "PRODUCT_VIEW",
  CATEGORY: "CATEGORY_VIEW",
  ORDER: "ORDER_VIEW",
  USER: "USER_VIEW",
};

export const ADMIN_MENU = [
  {
    to: "/admin/dashboard",
    label: "Dashboard",
    icon: "dashboard",
    tone: "orange",
    permission: ADMIN_PERMS.DASHBOARD,
  },
  {
    to: "/admin/product",
    label: "Sản phẩm",
    icon: "product",
    tone: "blue",
    permission: ADMIN_PERMS.PRODUCT,
  },
  {
    to: "/admin/category",
    label: "Danh mục",
    icon: "category",
    tone: "purple",
    permission: ADMIN_PERMS.CATEGORY,
  },
  {
    to: "/admin/order",
    label: "Đơn hàng",
    icon: "order",
    tone: "green",
    permission: ADMIN_PERMS.ORDER,
  },
  {
    to: "/admin/user",
    label: "Tài khoản",
    icon: "user",
    tone: "teal",
    permission: ADMIN_PERMS.USER,
  },
];

function matchesPermission(required, granted) {
  if (!required || !granted) return false;
  return String(required).trim().toUpperCase() === String(granted).trim().toUpperCase();
}

export function normalizeUserPermissions(user) {
  if (!user) return [];
  const raw = user.permissions;
  if (Array.isArray(raw)) return raw.map(String);
  return [];
}

export function userHasPermission(user, required) {
  if (!user || !required) return false;
  if (userIsAdmin(user)) return true;
  return normalizeUserPermissions(user).some((p) => matchesPermission(required, p));
}

export function filterAdminMenu(user) {
  return ADMIN_MENU.filter((item) => userHasPermission(user, item.permission));
}

export function resolveDefaultAdminRoute(user) {
  const items = filterAdminMenu(user);
  return items[0]?.to || null;
}

export function userIsStaff(user) {
  return userHasAnyRole(user, [ROLE_STAFF]);
}

export function userCanAccessPanel(user) {
  if (!user) return false;
  if (user.panelAccess === true) return true;
  if (userIsAdmin(user) || userIsStaff(user)) return true;
  return normalizeUserPermissions(user).length > 0;
}

export function panelRoleLabel(user) {
  if (userIsAdmin(user)) return "Quản trị viên";
  if (userIsStaff(user)) return "Nhân viên";
  if (userCanAccessPanel(user)) return "Nhân viên";
  return "Thành viên";
}
