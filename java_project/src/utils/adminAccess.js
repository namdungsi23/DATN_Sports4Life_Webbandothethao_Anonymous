import { ROLE_ADMIN, ROLE_STAFF, userHasAnyRole, userIsAdmin } from "./roles";

/** Admin panel permission keys — align with PermissionName in Sport4L DB */
export const ADMIN_PERMS = {
  DASHBOARD: "DASHBOARD_VIEW",
  PRODUCT: "PRODUCT_VIEW",
  CATEGORY: "CATEGORY_VIEW",
  ORDER: "ORDER_VIEW",
  ORDER_UPDATE: "ORDER_UPDATE",
  USER: "USER_VIEW",
  VOUCHER: "VOUCHER_VIEW",
  VOUCHER_CREATE: "VOUCHER_CREATE",
  VOUCHER_UPDATE: "VOUCHER_UPDATE",
  VOUCHER_DELETE: "VOUCHER_DELETE",
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
    to: "/admin/invoices",
    label: "Hóa đơn",
    icon: "chart",
    tone: "orange",
    permission: ADMIN_PERMS.ORDER,
  },
  {
    to: "/admin/voucher",
    label: "Khuyến mãi",
    icon: "package",
    tone: "purple",
    permission: ADMIN_PERMS.VOUCHER,
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

export function userCanWriteCatalog(user) {
  if (!user) return false;
  if (user.canWriteCatalog === true) return true;
  return userIsAdmin(user);
}

export function userCanUpdateOrders(user) {
  if (!user) return false;
  if (userIsAdmin(user)) return true;
  return userHasPermission(user, ADMIN_PERMS.ORDER_UPDATE);
}

export function userCanWriteVouchers(user) {
  if (!user) return false;
  if (userIsAdmin(user)) return true;
  return userHasPermission(user, ADMIN_PERMS.VOUCHER_CREATE)
    || userHasPermission(user, ADMIN_PERMS.VOUCHER_UPDATE);
}

export const ORDER_STATUS_LABELS = {
  PENDING: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  SHIPPING: "Đang giao",
  DELIVERED: "Đã giao",
  CANCELLED: "Đã hủy",
};

export const PAYMENT_STATUS_LABELS = {
  UNPAID: "Chưa thanh toán",
  PAID: "Đã thanh toán",
};

export const SHIPPING_STATUS_LABELS = {
  PENDING: "Chờ lấy hàng",
  PICKED_UP: "Đã lấy hàng",
  IN_TRANSIT: "Đang vận chuyển",
  OUT_FOR_DELIVERY: "Đang giao",
  DELIVERED: "Đã giao hàng",
  FAILED: "Giao thất bại",
  RETURNED: "Hoàn trả",
};

export function orderStatusBadgeClass(status) {
  switch (status) {
    case "PENDING":
      return "bg-warning text-dark";
    case "CONFIRMED":
      return "bg-info text-dark";
    case "SHIPPING":
      return "bg-primary";
    case "DELIVERED":
      return "bg-success";
    case "CANCELLED":
      return "bg-danger";
    default:
      return "bg-secondary";
  }
}

export function panelRoleLabel(user) {
  if (userIsAdmin(user)) return "Quản trị viên";
  if (userIsStaff(user)) return "Nhân viên";
  if (userCanAccessPanel(user)) return "Nhân viên";
  return "Thành viên";
}
