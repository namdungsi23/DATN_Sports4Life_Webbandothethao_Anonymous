/**
 * Chọn / validate biến thể theo màu + size (dùng khi SP có từ 2 biến thể trở lên).
 */

export function normalizeAttr(value) {
  if (value == null) return "";
  return String(value).trim();
}

export function isPlaceholderAttr(value) {
  const v = normalizeAttr(value).toLowerCase();
  return !v || v === "default" || v === "mặc định";
}

/** SP có biến thể thật (≥2) — lúc đó bắt buộc chọn màu/size theo DB. */
export function hasRealVariants(variants) {
  return Array.isArray(variants) && variants.length >= 2;
}

export function uniqueColors(variants) {
  return [
    ...new Set(
      (variants || [])
        .map((v) => normalizeAttr(v.color))
        .filter((c) => c && !isPlaceholderAttr(c))
    ),
  ];
}

export function uniqueSizes(variants) {
  return [
    ...new Set(
      (variants || [])
        .map((v) => normalizeAttr(v.size))
        .filter((s) => s && !isPlaceholderAttr(s))
    ),
  ];
}

/** Size còn bán được với màu đang chọn (hoặc mọi size nếu chưa chọn màu). */
export function sizesForColor(variants, color) {
  const list = variants || [];
  const c = normalizeAttr(color);
  const filtered = c
    ? list.filter((v) => normalizeAttr(v.color) === c)
    : list;
  return [
    ...new Set(
      filtered
        .map((v) => normalizeAttr(v.size))
        .filter((s) => s && !isPlaceholderAttr(s))
    ),
  ];
}

/** Màu còn bán được với size đang chọn. */
export function colorsForSize(variants, size) {
  const list = variants || [];
  const s = normalizeAttr(size);
  const filtered = s
    ? list.filter((v) => normalizeAttr(v.size) === s)
    : list;
  return [
    ...new Set(
      filtered
        .map((v) => normalizeAttr(v.color))
        .filter((c) => c && !isPlaceholderAttr(c))
    ),
  ];
}

export function findVariant(variants, { color, size } = {}) {
  const list = variants || [];
  const c = normalizeAttr(color);
  const s = normalizeAttr(size);
  return (
    list.find((v) => {
      const vc = normalizeAttr(v.color);
      const vs = normalizeAttr(v.size);
      const colorOk = !c || vc === c || (isPlaceholderAttr(vc) && !c);
      const sizeOk = !s || vs === s || (isPlaceholderAttr(vs) && !s);
      // Khi có chọn màu/size thật: phải khớp đúng
      if (c && !isPlaceholderAttr(c) && vc !== c) return false;
      if (s && !isPlaceholderAttr(s) && vs !== s) return false;
      return colorOk && sizeOk;
    }) || null
  );
}

export function isVariantInStock(variant) {
  if (!variant) return false;
  if (variant.inStock === false) return false;
  const stock = Number(variant.stock ?? variant.quantity ?? 0);
  return Number.isFinite(stock) ? stock > 0 : !!variant.inStock;
}

/**
 * @returns {{ ok: boolean, errors: Record<string, string>, variant: object|null }}
 */
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

  if (!real) {
    const fallback =
      variants.find((v) => v.isDefault) || variants[0] || null;
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
    return {
      ok: Object.keys(errors).length === 0,
      errors,
      variant: fallback,
    };
  }

  if (colors.length && !normalizeAttr(color)) {
    errors.color = "Vui lòng chọn màu sắc.";
  }
  if (sizes.length && !normalizeAttr(size)) {
    errors.size = "Vui lòng chọn kích cỡ.";
  }

  const variant =
    Object.keys(errors).length === 0
      ? findVariant(variants, { color, size })
      : null;

  if (!errors.color && !errors.size && !variant) {
    errors.variant = "Không có biến thể phù hợp với màu/size đã chọn.";
  }

  if (variant && !isVariantInStock(variant)) {
    errors.stock = "Biến thể này đã hết hàng. Vui lòng chọn màu/size khác.";
  }

  const qty = Math.max(1, Math.floor(Number(quantity) || 1));
  if (qty < 1) {
    errors.quantity = "Số lượng phải từ 1 trở lên.";
  }
  if (variant) {
    const stock = Number(variant.stock ?? variant.quantity ?? 0);
    if (Number.isFinite(stock) && stock > 0 && qty > stock) {
      errors.quantity = `Chỉ còn ${stock} sản phẩm cho biến thể này.`;
    }
  }

  return {
    ok: Object.keys(errors).length === 0,
    errors,
    variant: variant || null,
  };
}
