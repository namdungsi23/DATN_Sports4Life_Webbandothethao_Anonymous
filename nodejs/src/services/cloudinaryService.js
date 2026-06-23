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
