export const FALLBACK_PRODUCT_IMAGE =
  "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600&q=80";

function toUrlList(images) {
  if (!Array.isArray(images)) return [];
  return images
    .map((img) => (typeof img === "string" ? img : img?.imageUrl || img?.url))
    .filter(Boolean);
}

/**
 * Nghiệp vụ FE: chỉ dùng URL ảnh Backend đã trả từ SQL (ProductImages).
 * Không tự ghép / list Cloudinary.
 */
export function resolveVariantImages(variant) {
  if (!variant) return [];

  const fromArray = toUrlList(variant.images);
  if (fromArray.length) return fromArray.slice(0, 4);

  if (variant.image) return [variant.image];
  return [];
}

export function resolveProductImage(product) {
  if (!product) return FALLBACK_PRODUCT_IMAGE;

  const direct =
    product.image ||
    product.imageUrl ||
    (typeof product.images?.[0] === "string" ? product.images[0] : product.images?.[0]?.imageUrl) ||
    product.gallery?.[0];

  return direct || FALLBACK_PRODUCT_IMAGE;
}

export function normalizeProduct(product) {
  if (!product || typeof product !== "object") return product;

  const variants = Array.isArray(product.variants)
    ? product.variants.map((v) => {
        const images = resolveVariantImages(v);
        return {
          ...v,
          images,
          image: images[0] || v.image || null,
        };
      })
    : product.variants;

  // Gallery SP ưu tiên biến thể mặc định (không gộp mọi biến thể)
  const def =
    (Array.isArray(variants) && variants.find((v) => v.isDefault)) ||
    (Array.isArray(variants) && variants[0]) ||
    null;
  const fromDefault = resolveVariantImages(def);

  const gallery =
    (fromDefault.length ? fromDefault : null) ||
    (Array.isArray(product.gallery) && product.gallery.length
      ? product.gallery.filter(Boolean).slice(0, 4)
      : null) ||
    toUrlList(product.images).slice(0, 4) ||
    [];

  const uniqueGallery = [...new Set(gallery.filter(Boolean))].slice(0, 4);
  if (!uniqueGallery.length) {
    const one = resolveProductImage(product);
    if (one) uniqueGallery.push(one);
  }

  return {
    ...product,
    variants,
    gallery: uniqueGallery,
    image: uniqueGallery[0] || resolveProductImage(product),
  };
}

export function normalizeProductsResponse(data) {
  if (!data) return data;

  if (data.products) {
    const content = (data.products.content ?? []).map(normalizeProduct);
    return {
      ...data,
      products: { ...data.products, content },
    };
  }

  const content = (data.content ?? []).map(normalizeProduct);
  return { ...data, content };
}

export function normalizeProductDetailResponse(data) {
  if (!data?.product) return data;
  return {
    ...data,
    product: normalizeProduct(data.product),
    related: (data.related ?? []).map(normalizeProduct),
  };
}
