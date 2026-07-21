/** Shared form validators — dùng cho mọi chỗ bắt buộc nhập */

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;
/** SĐT VN: 0xxxxxxxxx (10 số) hoặc +84xxxxxxxxx */
const PHONE_REGEX = /^(0\d{9}|\+84\d{9})$/;

export const isBlank = (value) => value == null || String(value).trim() === "";

export const trim = (value) => String(value ?? "").trim();

export const isValidEmail = (value) => EMAIL_REGEX.test(trim(value));

export const normalizePhone = (value) =>
  String(value ?? "")
    .replace(/[\s.\-()]/g, "")
    .trim();

export const isValidPhone = (value) => PHONE_REGEX.test(normalizePhone(value));

export const minLength = (value, len) => trim(value).length >= len;

export const maxLength = (value, len) => trim(value).length <= len;

/**
 * @typedef {{ ok: boolean, errors: Record<string, string>, values: Record<string, unknown> }} ValidationResult
 */

/**
 * Chạy rules theo field.
 * Rule: string shorthand hoặc { type, message?, min?, max? }
 * Shorthand: 'required' | 'email' | 'phone' | 'password' | 'username'
 *
 * @param {Record<string, unknown>} data
 * @param {Record<string, Array<string|object>>} schema
 * @returns {ValidationResult}
 */
export function runValidation(data, schema) {
  const errors = {};
  const values = {};

  for (const [field, rules] of Object.entries(schema || {})) {
    const raw = data?.[field];
    let value = raw;
    if (typeof raw === "string") value = raw.trim();
    values[field] = value;

    for (const rule of rules || []) {
      const r = typeof rule === "string" ? { type: rule } : rule || {};
      const type = r.type;
      const msg = r.message;

      if (type === "required") {
        if (isBlank(value) && value !== 0) {
          errors[field] = msg || "Trường này là bắt buộc.";
          break;
        }
      } else if (type === "email") {
        if (!isBlank(value) && !isValidEmail(value)) {
          errors[field] = msg || "Email không hợp lệ.";
          break;
        }
      } else if (type === "phone") {
        if (!isBlank(value) && !isValidPhone(value)) {
          errors[field] = msg || "Số điện thoại không hợp lệ (VD: 09xxxxxxxx).";
          break;
        }
      } else if (type === "username") {
        if (!isBlank(value)) {
          if (!minLength(value, r.min ?? 3)) {
            errors[field] = msg || `Tên đăng nhập tối thiểu ${r.min ?? 3} ký tự.`;
            break;
          }
          if (!maxLength(value, r.max ?? 50)) {
            errors[field] = msg || `Tên đăng nhập tối đa ${r.max ?? 50} ký tự.`;
            break;
          }
          if (!/^[a-zA-Z0-9._-]+$/.test(String(value))) {
            errors[field] = msg || "Username chỉ gồm chữ, số, dấu . _ -";
            break;
          }
        }
      } else if (type === "password") {
        if (!isBlank(value)) {
          const min = r.min ?? 8;
          if (!minLength(value, min)) {
            errors[field] = msg || `Mật khẩu tối thiểu ${min} ký tự.`;
            break;
          }
          if (!/[A-Za-z]/.test(String(value)) || !/\d/.test(String(value))) {
            errors[field] = msg || "Mật khẩu phải gồm ít nhất 1 chữ cái và 1 chữ số.";
            break;
          }
        }
      } else if (type === "min") {
        if (!isBlank(value) && !minLength(value, r.min ?? 1)) {
          errors[field] = msg || `Tối thiểu ${r.min ?? 1} ký tự.`;
          break;
        }
      } else if (type === "max") {
        if (!isBlank(value) && !maxLength(value, r.max ?? 255)) {
          errors[field] = msg || `Tối đa ${r.max ?? 255} ký tự.`;
          break;
        }
      } else if (type === "match") {
        const other = data?.[r.field];
        if (String(value ?? "") !== String(other ?? "")) {
          errors[field] = msg || "Giá trị không khớp.";
          break;
        }
      } else if (type === "number") {
        const n = Number(value);
        if (value !== "" && value != null && !Number.isFinite(n)) {
          errors[field] = msg || "Giá trị không hợp lệ.";
          break;
        }
        if (Number.isFinite(n)) {
          if (r.min != null && n < r.min) {
            errors[field] = msg || `Phải ≥ ${r.min}.`;
            break;
          }
          if (r.max != null && n > r.max) {
            errors[field] = msg || `Phải ≤ ${r.max}.`;
            break;
          }
          if (r.gt != null && !(n > r.gt)) {
            errors[field] = msg || `Phải lớn hơn ${r.gt}.`;
            break;
          }
          values[field] = n;
        }
      } else if (type === "fileImage") {
        if (value instanceof File) {
          if (!String(value.type || "").startsWith("image/")) {
            errors[field] = msg || "Chỉ chấp nhận file ảnh.";
            break;
          }
          const maxBytes = r.maxBytes ?? 5 * 1024 * 1024;
          if (value.size > maxBytes) {
            errors[field] = msg || "Ảnh tối đa 5MB.";
            break;
          }
        }
      }
    }
  }

  return {
    ok: Object.keys(errors).length === 0,
    errors,
    values,
  };
}

export function firstError(errors) {
  const vals = Object.values(errors || {});
  return vals.length ? String(vals[0]) : "";
}

/**
 * Đọc lỗi từ API response (ApiExceptionHandler).
 * @returns {{ message: string, errors: Record<string, string>, status?: number }}
 */
export function getApiError(err, fallback = "Có lỗi xảy ra. Vui lòng thử lại.") {
  const data = err?.response?.data;
  const status = err?.response?.status;
  const errors =
    data?.errors && typeof data.errors === "object" && !Array.isArray(data.errors)
      ? data.errors
      : {};

  let message =
    (typeof data?.message === "string" && data.message) ||
    (typeof data?.error === "string" && data.error) ||
    (typeof err?.message === "string" && err.message) ||
    fallback;

  if (!Object.keys(errors).length && Array.isArray(data?.errors)) {
    // fallback nếu BE trả mảng
    message = data.errors[0]?.defaultMessage || data.errors[0]?.message || message;
  }

  const firstField = firstError(errors);
  if (firstField && (!data?.message || data.message === "Dữ liệu không hợp lệ.")) {
    message = firstField;
  }

  return { message: String(message), errors, status };
}
