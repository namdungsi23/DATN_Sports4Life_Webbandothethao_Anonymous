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
};

export const DEFAULT_BRAND_NAMES = [
  "Nike",
  "Adidas",
  "Puma",
  "New Balance",
  "Converse",
  "Asics",
  "Reebok",
  "Vans",
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
