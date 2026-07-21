export const FALLBACK_PRODUCT_IMAGE =
  "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600&q=80";

const CLOUDINARY_PRODUCT_BASE =
  "https://res.cloudinary.com/pnam233/image/upload/product";

/** URL Cloudinary tạm (1 ảnh) theo category + productId. */
export function cloudinaryProductUrl(categoryId, productId) {
  if (!categoryId || productId == null) return null;
  const seq = ((Number(productId) - 1) % 10) + 1;
  return `${CLOUDINARY_PRODUCT_BASE}/${String(categoryId).toLowerCase()}_${seq}.jpg`;
}

function toUrlList(images) {
  if (!Array.isArray(images)) return [];
  return images
    .map((img) => (typeof img === "string" ? img : img?.imageUrl || img?.url))
    .filter(Boolean);
}

/** Tạm thời: ảnh DB của biến thể, hoặc 1 ảnh Cloudinary fallback. */
export function resolveVariantImages(variant, product) {
  if (!variant) return [];

  const fromArray = toUrlList(variant.images);
  if (fromArray.length) return fromArray;

  if (variant.image) return [variant.image];

  const temp = cloudinaryProductUrl(product?.categoryId, product?.id ?? variant.productId);
  return temp ? [temp] : [];
}

export function resolveProductImage(product) {
  if (!product) return FALLBACK_PRODUCT_IMAGE;

  const direct =
    product.image ||
    product.imageUrl ||
    (typeof product.images?.[0] === "string" ? product.images[0] : product.images?.[0]?.imageUrl) ||
    product.gallery?.[0];

  if (direct) return direct;

  return cloudinaryProductUrl(product.categoryId, product.id) || FALLBACK_PRODUCT_IMAGE;
}

export function normalizeProduct(product) {
  if (!product || typeof product !== "object") return product;

  const variants = Array.isArray(product.variants)
    ? product.variants.map((v) => {
        const images = resolveVariantImages(v, product);
        return {
          ...v,
          images,
          image: images[0] || v.image || null,
        };
      })
    : product.variants;

  const gallery =
    (Array.isArray(product.gallery) && product.gallery.length
      ? product.gallery.filter(Boolean)
      : null) ||
    toUrlList(product.images) ||
    [];

  const uniqueGallery = [...new Set(gallery.filter(Boolean))];
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
