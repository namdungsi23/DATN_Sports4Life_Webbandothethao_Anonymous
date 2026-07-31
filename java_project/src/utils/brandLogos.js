/** Logo thương hiệu (SVG Wikimedia) — dùng cho BrandStrip chạy nổi + trang Brands */

export const BRAND_LOGO_BY_KEY = {
  nike: "https://upload.wikimedia.org/wikipedia/commons/a/a6/Logo_NIKE.svg",
  adidas: "https://upload.wikimedia.org/wikipedia/commons/2/20/Adidas_Logo.svg",
  puma: "https://upload.wikimedia.org/wikipedia/commons/f/fd/Puma_logo.svg",
  "new balance": "https://upload.wikimedia.org/wikipedia/commons/e/ea/New_Balance_logo.svg",
  converse: "https://upload.wikimedia.org/wikipedia/commons/3/30/Converse_logo.svg",
  asics: "https://upload.wikimedia.org/wikipedia/commons/b/b1/Asics_Logo.svg",
  reebok: "https://upload.wikimedia.org/wikipedia/commons/2/29/Reebok_logo.svg",
  vans: "https://upload.wikimedia.org/wikipedia/commons/a/a9/Vans-logo.svg",
  mizuno: "https://upload.wikimedia.org/wikipedia/commons/8/8a/Mizuno-logo.svg",
  "under armour": "https://upload.wikimedia.org/wikipedia/commons/4/44/Under_Armour_logo.svg",
  fila: "https://upload.wikimedia.org/wikipedia/commons/7/72/Fila_logo.svg",
  skechers: "https://upload.wikimedia.org/wikipedia/commons/1/1a/Skechers_logo.svg",
  columbia: "https://upload.wikimedia.org/wikipedia/commons/4/4b/Columbia_Sportswear_Company_logo.svg",
  "the north face":
    "https://upload.wikimedia.org/wikipedia/commons/e/e9/The_North_Face_Logo.svg",
  lululemon: "https://upload.wikimedia.org/wikipedia/commons/2/22/Lululemon_Athletica_logo.svg",
  champion: "https://upload.wikimedia.org/wikipedia/commons/6/6e/Champion_logo.svg",
  jordan: "https://upload.wikimedia.org/wikipedia/en/3/37/Jumpman_logo.svg",
  salomon: "https://upload.wikimedia.org/wikipedia/commons/5/5a/Salomon_Group_logo.svg",
  hoka: "https://upload.wikimedia.org/wikipedia/commons/5/5d/Hoka_One_One_logo.svg",
  brooks: "https://upload.wikimedia.org/wikipedia/commons/8/8a/Brooks_Sports_logo.svg",
  "li-ning": "https://upload.wikimedia.org/wikipedia/commons/1/1a/Li-Ning_logo.svg",
  anta: "https://upload.wikimedia.org/wikipedia/commons/8/8e/ANTA_Sports_logo.svg",
  kappa: "https://upload.wikimedia.org/wikipedia/commons/b/b0/Kappa_Logo.svg",
  umbro: "https://upload.wikimedia.org/wikipedia/commons/d/d8/Umbro_logo.svg",
  lotto: "https://upload.wikimedia.org/wikipedia/commons/9/9d/Lotto_Sport_Italia_logo.svg",
  diadora: "https://upload.wikimedia.org/wikipedia/commons/3/36/Diadora_logo.svg",
};

/** Danh sách mặc định cho dải logo chạy nổi (đủ dài để animation mượt) */
export const DEFAULT_BRAND_NAMES = [
  "Nike",
  "Adidas",
  "Puma",
  "New Balance",
  "Under Armour",
  "Asics",
  "Reebok",
  "Converse",
  "Vans",
  "Mizuno",
  "Fila",
  "Jordan",
  "Hoka",
  "Brooks",
  "Salomon",
  "Skechers",
  "Columbia",
  "The North Face",
  "Lululemon",
  "Champion",
  "Li-Ning",
  "Anta",
  "Kappa",
  "Umbro",
  "Diadora",
  "Lotto",
];

export function resolveBrandLogo(name) {
  const normalized = String(name || "").trim().toLowerCase();
  if (!normalized) return "";

  if (BRAND_LOGO_BY_KEY[normalized]) return BRAND_LOGO_BY_KEY[normalized];

  const matchedKey = Object.keys(BRAND_LOGO_BY_KEY).find(
    (key) => normalized.includes(key) || key.includes(normalized)
  );
  return matchedKey ? BRAND_LOGO_BY_KEY[matchedKey] : "";
}

/** Gộp brand từ API với danh sách mặc định (ưu tiên tên chuẩn, không trùng). */
export function mergeBrandNames(apiNames = []) {
  const seen = new Set();
  const out = [];

  const push = (name) => {
    const clean = String(name || "").trim();
    if (!clean) return;
    const key = clean.toLowerCase();
    if (seen.has(key)) return;
    seen.add(key);
    out.push(clean);
  };

  for (const name of DEFAULT_BRAND_NAMES) push(name);
  for (const name of apiNames) push(name);
  return out;
}
