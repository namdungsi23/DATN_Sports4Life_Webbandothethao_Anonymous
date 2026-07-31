/** Map hạng thành viên → tone / nhãn hiển thị. */

const TONE_BY_KEY = {
  bronze: "bronze",
  silver: "silver",
  gold: "gold",
  platinum: "platinum",
  diamond: "diamond",
  dong: "bronze",
  bac: "silver",
  vang: "gold",
  "kim cuong": "diamond",
  "kim cương": "diamond",
};

const META = {
  bronze: { tone: "bronze", icon: "🥉", label: "Bronze" },
  silver: { tone: "silver", icon: "🥈", label: "Silver" },
  gold: { tone: "gold", icon: "🥇", label: "Gold" },
  platinum: { tone: "platinum", icon: "💎", label: "Platinum" },
  diamond: { tone: "diamond", icon: "💠", label: "Diamond" },
  default: { tone: "default", icon: "⭐", label: "Thành viên" },
};

function normalizeKey(name) {
  return String(name || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim();
}

export function resolveRankTone(rankName) {
  const key = normalizeKey(rankName);
  if (!key) return "default";
  for (const [k, tone] of Object.entries(TONE_BY_KEY)) {
    if (key.includes(k)) return tone;
  }
  return "default";
}

export function rankVisual(rankName) {
  const tone = resolveRankTone(rankName);
  const meta = META[tone] || META.default;
  return {
    tone: meta.tone,
    icon: meta.icon,
    label: rankName || meta.label,
  };
}

export function formatRankDiscount(v) {
  if (v == null || v === "") return null;
  const n = Number(v);
  if (!Number.isFinite(n) || n <= 0) return null;
  return Number.isInteger(n) ? `${n}%` : `${n}%`;
}
