/**
 * Chọn / validate biến thể theo màu + size (dùng khi SP có từ 2 biến thể trở lên).
 * Size theo nghiệp vụ catalog:
 * - Giày: EU 39–43
 * - Áo/quần/áo khoác/bộ: S–XL
 * - Phụ kiện / balo / tất: Free (không bắt user chọn size)
 */

export function normalizeAttr(value) {
  if (value == null) return "";
  return String(value).trim();
}

export function isPlaceholderAttr(value) {
  const v = normalizeAttr(value).toLowerCase();
  return !v || v === "default" || v === "mặc định" || v === "mac dinh";
}

/** Size Free — phụ kiện/balo/tất: không hiện lưới chọn size. */
export function isFreeSize(value) {
  const v = normalizeAttr(value).toLowerCase().replace(/\s+/g, " ");
  return v === "free" || v === "free size" || v === "freesize";
}

const SIZE_RANK = {
  "39": 1, "40": 2, "41": 3, "42": 4, "43": 5, "44": 6,
  s: 10, m: 11, l: 12, xl: 13, xxl: 14,
  free: 100, "free size": 100, freesize: 100,
};

export function sortSizes(sizes) {
  return [...(sizes || [])].sort((a, b) => {
    const ka = SIZE_RANK[normalizeAttr(a).toLowerCase()] ?? 50;
    const kb = SIZE_RANK[normalizeAttr(b).toLowerCase()] ?? 50;
    if (ka !== kb) return ka - kb;
    return String(a).localeCompare(String(b), "vi");
  });
}

const COLOR_RANK = {
  đen: 1, den: 1, trắng: 2, trang: 2,
  "xanh navy": 3, navy: 3, đỏ: 4, do: 4,
  xám: 5, xam: 5, "xanh lá": 6, "xanh la": 6,
};

export function sortColors(colors) {
  return [...(colors || [])].sort((a, b) => {
    const ka = COLOR_RANK[normalizeAttr(a).toLowerCase()] ?? 50;
    const kb = COLOR_RANK[normalizeAttr(b).toLowerCase()] ?? 50;
    if (ka !== kb) return ka - kb;
    return String(a).localeCompare(String(b), "vi");
  });
}

export function hasRealVariants(variants) {
  return Array.isArray(variants) && variants.length >= 2;
}

export function uniqueColors(variants) {
  return sortColors([
    ...new Set(
      (variants || [])
        .map((v) => normalizeAttr(v.color))
        .filter((c) => c && !isPlaceholderAttr(c))
    ),
  ]);
}

export function uniqueSizes(variants) {
  return sortSizes([
    ...new Set(
      (variants || [])
        .map((v) => normalizeAttr(v.size))
        .filter((s) => s && !isPlaceholderAttr(s))
    ),
  ]);
}

export function sizesForColor(variants, color) {
  const list = variants || [];
  const c = normalizeAttr(color);
  const filtered = c ? list.filter((v) => normalizeAttr(v.color) === c) : list;
  return sortSizes([
    ...new Set(
      filtered
        .map((v) => normalizeAttr(v.size))
        .filter((s) => s && !isPlaceholderAttr(s))
    ),
  ]);
}

/** Size hiện trên UI — ẩn khi chỉ còn Free. */
export function selectableSizes(variants, color) {
  const sizes = sizesForColor(variants, color);
  if (!sizes.length) return [];
  if (sizes.every(isFreeSize)) return [];
  return sizes.filter((s) => !isFreeSize(s));
}

export function colorsForSize(variants, size) {
  const list = variants || [];
  const s = normalizeAttr(size);
  const filtered = s ? list.filter((v) => normalizeAttr(v.size) === s) : list;
  return sortColors([
    ...new Set(
      filtered
        .map((v) => normalizeAttr(v.color))
        .filter((c) => c && !isPlaceholderAttr(c))
    ),
  ]);
}

export function findVariant(variants, { color, size } = {}) {
  const list = variants || [];
  const c = normalizeAttr(color);
  const s = normalizeAttr(size);

  if (c && s) {
    const exact = list.find(
      (v) => normalizeAttr(v.color) === c && normalizeAttr(v.size) === s
    );
    if (exact) return exact;
    if (isFreeSize(s)) {
      return list.find((v) => normalizeAttr(v.color) === c && isFreeSize(v.size)) || null;
    }
    return null;
  }

  if (c) {
    const ofColor = list.filter((v) => normalizeAttr(v.color) === c);
    if (!ofColor.length) return null;
    return (
      ofColor.find((v) => isFreeSize(v.size)) ||
      ofColor.find((v) => v.isDefault) ||
      ofColor[0] ||
      null
    );
  }

  if (s) {
    return (
      list.find((v) => normalizeAttr(v.size) === s) ||
      (isFreeSize(s) ? list.find((v) => isFreeSize(v.size)) : null) ||
      null
    );
  }

  return list.find((v) => v.isDefault) || list[0] || null;
}

export function isVariantInStock(variant) {
  if (!variant) return false;
  if (variant.inStock === false) return false;
  const stock = Number(variant.stock ?? variant.quantity ?? 0);
  return Number.isFinite(stock) ? stock > 0 : !!variant.inStock;
}

export function validateVariantSelection({
  variants = [],
  color = "",
  size = "",
  quantity = 1,
} = {}) {
  const errors = {};
  const real = hasRealVariants(variants);
  const colors = uniqueColors(variants);
  const sizes = uniqueSizes(variants);
  const needsSize = sizes.some((s) => !isFreeSize(s));

  if (!real) {
    const fallback = variants.find((v) => v.isDefault) || variants[0] || null;
    const qty = Math.max(1, Math.floor(Number(quantity) || 1));
    if (fallback && !isVariantInStock(fallback)) {
      errors.stock = "Sản phẩm đã hết hàng.";
    }
    if (fallback) {
      const stock = Number(fallback.stock ?? fallback.quantity ?? 0);
      if (Number.isFinite(stock) && stock > 0 && qty > stock) {
        errors.quantity = `Chỉ còn ${stock} sản phẩm trong kho.`;
      }
    }
    return { ok: Object.keys(errors).length === 0, errors, variant: fallback };
  }

  if (colors.length && !normalizeAttr(color)) {
    errors.color = "Vui lòng chọn màu sắc.";
  }
  if (needsSize && !normalizeAttr(size)) {
    errors.size = "Vui lòng chọn kích cỡ.";
  }

  let effectiveSize = normalizeAttr(size);
  if (!effectiveSize && sizes.length && sizes.every(isFreeSize)) {
    effectiveSize = sizes[0];
  }

  const variant =
    Object.keys(errors).length === 0
      ? findVariant(variants, { color, size: effectiveSize })
      : null;

  if (!errors.color && !errors.size && !variant) {
    errors.variant = "Không có biến thể phù hợp với màu/size đã chọn.";
  }
  if (variant && !isVariantInStock(variant)) {
    errors.stock = "Biến thể này đã hết hàng. Vui lòng chọn màu/size khác.";
  }

  const qty = Math.max(1, Math.floor(Number(quantity) || 1));
  if (qty < 1) errors.quantity = "Số lượng phải từ 1 trở lên.";
  if (variant) {
    const stock = Number(variant.stock ?? variant.quantity ?? 0);
    if (Number.isFinite(stock) && stock > 0 && qty > stock) {
      errors.quantity = `Chỉ còn ${stock} sản phẩm cho biến thể này.`;
    }
  }

  return { ok: Object.keys(errors).length === 0, errors, variant: variant || null };
}