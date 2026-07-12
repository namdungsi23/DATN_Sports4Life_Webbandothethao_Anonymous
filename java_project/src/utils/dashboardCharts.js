/**
 * Helpers cho biểu đồ / KPI trang admin dashboard & doanh thu.
 */

const STATUS_THEME = {
  PENDING: { color: "#f59e0b", label: "Chờ xác nhận" },
  CONFIRMED: { color: "#3b82f6", label: "Đã xác nhận" },
  SHIPPING: { color: "#6366f1", label: "Đang giao" },
  DELIVERED: { color: "#10b981", label: "Đã giao" },
  CANCELLED: { color: "#ef4444", label: "Đã hủy" },
};

const CATEGORY_PALETTE = [
  "#0d9488",
  "#2563eb",
  "#ea580c",
  "#7c3aed",
  "#db2777",
  "#0891b2",
  "#65a30d",
  "#dc2626",
];

export function formatPrice(value) {
  const n = Number(value || 0);
  if (!Number.isFinite(n)) return "0";
  return Math.round(n).toLocaleString("vi-VN");
}

export function formatCompact(value) {
  const n = Number(value || 0);
  if (!Number.isFinite(n)) return "0";
  if (Math.abs(n) >= 1_000_000_000) return `${(n / 1_000_000_000).toFixed(1)} tỷ`;
  if (Math.abs(n) >= 1_000_000) return `${(n / 1_000_000).toFixed(1)} Tr`;
  if (Math.abs(n) >= 1_000) return `${(n / 1_000).toFixed(1)} K`;
  return Math.round(n).toLocaleString("vi-VN");
}

export function sumRevenue(items, key = "revenue") {
  if (!Array.isArray(items) || !items.length) return 0;
  return items.reduce((acc, item) => acc + Number(item?.[key] || 0), 0);
}

export function barPercent(value, max) {
  const v = Number(value || 0);
  const m = Number(max || 0);
  if (!m || m <= 0) return 0;
  return Math.max(0, Math.min(100, Math.round((v / m) * 1000) / 10));
}

export function categoryColor(index = 0) {
  return CATEGORY_PALETTE[Math.abs(Number(index) || 0) % CATEGORY_PALETTE.length];
}

export function statusTheme(status) {
  return STATUS_THEME[status] || { color: "#9ca3af", label: status || "—" };
}

export function rankMeta(index) {
  const i = Number(index) || 0;
  if (i === 0) return { badge: "🥇", tone: "gold" };
  if (i === 1) return { badge: "🥈", tone: "silver" };
  if (i === 2) return { badge: "🥉", tone: "bronze" };
  return { badge: `#${i + 1}`, tone: "muted" };
}

/**
 * Tạo CSS conic-gradient cho donut phân bố trạng thái đơn.
 * @param {Array<{ status: string, orderCount: number }>} distribution
 */
export function buildConicGradient(distribution) {
  const items = Array.isArray(distribution) ? distribution : [];
  const total = items.reduce((acc, row) => acc + Number(row.orderCount || 0), 0);
  if (!total) {
    return "conic-gradient(#e5e7eb 0deg 360deg)";
  }

  let cursor = 0;
  const parts = [];
  items.forEach((row) => {
    const count = Number(row.orderCount || 0);
    if (count <= 0) return;
    const deg = (count / total) * 360;
    const color = statusTheme(row.status).color;
    const start = cursor;
    const end = cursor + deg;
    parts.push(`${color} ${start}deg ${end}deg`);
    cursor = end;
  });

  if (!parts.length) {
    return "conic-gradient(#e5e7eb 0deg 360deg)";
  }
  return `conic-gradient(${parts.join(", ")})`;
}

export { STATUS_THEME, CATEGORY_PALETTE };
