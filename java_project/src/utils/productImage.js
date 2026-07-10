export const FALLBACK_PRODUCT_IMAGE =
  "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600&q=80";

const CLOUDINARY_PRODUCT_BASE =
  "https://res.cloudinary.com/pnam233/image/upload/product";

export function cloudinaryProductUrl(categoryId, productId) {
  if (!categoryId || productId == null) return null;
  const seq = ((Number(productId) - 1) % 10) + 1;
  return `${CLOUDINARY_PRODUCT_BASE}/${String(categoryId).toLowerCase()}_${seq}.jpg`;
}

export function resolveProductImage(product) {
  if (!product) return FALLBACK_PRODUCT_IMAGE;

  const direct =
    product.image ||
    product.imageUrl ||
    product.images?.find((img) => img?.isDefault)?.imageUrl ||
    product.images?.[0]?.imageUrl;

  if (direct) return direct;

  return cloudinaryProductUrl(product.categoryId, product.id) || FALLBACK_PRODUCT_IMAGE;
}

export function normalizeProduct(product) {
  if (!product || typeof product !== "object") return product;
  return {
    ...product,
    image: resolveProductImage(product),
  };
}

export function normalizeProductsResponse(data) {
  if (!data) return data;

  if (data.products) {
    const content = (data.products.content ?? []).map(normalizeProduct);
    return {
      ...data,
      products: { ...data.products, content },
      suggestions: (data.suggestions ?? []).map(normalizeProduct),
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
