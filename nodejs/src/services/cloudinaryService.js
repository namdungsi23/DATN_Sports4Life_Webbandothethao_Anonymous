import { v2 as cloudinary } from "cloudinary";
import { config } from "../config/index.js";

let configured = false;

function ensure() {
  const { cloudName, apiKey, apiSecret } = config.cloudinary;
  if (cloudName && apiKey && apiSecret) {
    if (!configured) {
      cloudinary.config({ cloud_name: cloudName, api_key: apiKey, api_secret: apiSecret });
      configured = true;
    }
    return true;
  }
  return false;
}

/**
 * @param {Buffer} buffer
 * @param {string} originalName
 * @returns {Promise<string>} secure_url
 */
export async function uploadProductImage(buffer, originalName) {
  if (!ensure()) {
    throw new Error("Cloudinary is not configured; set CLOUDINARY_* in .env");
  }
  const base = (originalName || "image").replace(/\.[^.]+$/, "") || "image";
  return new Promise((resolve, reject) => {
    const stream = cloudinary.uploader.upload_stream(
      {
        folder: "product",
        resource_type: "image",
        format: "jpg",
        public_id: base,
        overwrite: true,
      },
      (err, result) => {
        if (err) reject(err);
        else resolve(String(result?.secure_url || result?.url));
      }
    );
    stream.end(buffer);
  });
}

/**
 * Lấy đầy đủ URL ảnh trong folder Cloudinary (vd: product, product/c001).
 * @param {string} [folder="product"]
 * @param {string} [prefix] — lọc theo prefix public_id (vd: "product/c001")
 * @returns {Promise<string[]>}
 */
export async function listProductImageUrls(folder = "product", prefix) {
  if (!ensure()) {
    throw new Error("Cloudinary is not configured; set CLOUDINARY_* in .env");
  }

  const expression = prefix
    ? `folder:${folder} AND public_id:${prefix}*`
    : `folder:${folder}`;

  const urls = [];
  let nextCursor;

  do {
    const result = await cloudinary.search
      .expression(expression)
      .sort_by("public_id", "asc")
      .max_results(100)
      .next_cursor(nextCursor)
      .execute();

    for (const resource of result.resources || []) {
      const url = resource.secure_url || resource.url;
      if (url && !urls.includes(url)) urls.push(url);
    }
    nextCursor = result.next_cursor;
  } while (nextCursor);

  return urls;
}

/**
 * Sinh đầy đủ mảng URL theo pattern category (fallback khi chưa list API).
 * @param {string} categoryId — vd: c001
 * @returns {string[]}
 */
export function buildCategoryImageUrlArray(categoryId, count = 10) {
  const cloudName = config.cloudinary.cloudName || "pnam233";
  if (!categoryId) return [];
  const cat = String(categoryId).toLowerCase().trim();
  const urls = [];
  for (let seq = 1; seq <= count; seq++) {
    urls.push(`https://res.cloudinary.com/${cloudName}/image/upload/product/${cat}_${seq}.jpg`);
  }
  return urls;
}
