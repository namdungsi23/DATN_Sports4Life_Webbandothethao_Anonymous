/** Dữ liệu + logic gợi ý size từ giới tính / chiều cao / cân nặng. Không dùng DB. */

export const GENDER_OPTIONS = [
  { value: "male", label: "Nam" },
  { value: "female", label: "Nữ" },
];

// value = giá trị đại diện (giữa khoảng) để tính toán
export const HEIGHT_OPTIONS = [
  { value: 152, label: "Dưới 155 cm" },
  { value: 157, label: "155 – 160 cm" },
  { value: 162, label: "160 – 165 cm" },
  { value: 167, label: "165 – 170 cm" },
  { value: 172, label: "170 – 175 cm" },
  { value: 177, label: "175 – 180 cm" },
  { value: 182, label: "180 – 185 cm" },
  { value: 188, label: "Trên 185 cm" },
];

export const WEIGHT_OPTIONS = [
  { value: 43, label: "Dưới 45 kg" },
  { value: 47, label: "45 – 50 kg" },
  { value: 52, label: "50 – 55 kg" },
  { value: 57, label: "55 – 60 kg" },
  { value: 62, label: "60 – 65 kg" },
  { value: 67, label: "65 – 70 kg" },
  { value: 72, label: "70 – 75 kg" },
  { value: 80, label: "75 – 85 kg" },
  { value: 90, label: "Trên 85 kg" },
];

const APPAREL_ORDER = ["XS", "S", "M", "L", "XL", "XXL", "XXXL"];

/** Nhận loại sản phẩm từ danh sách size của sản phẩm (nếu có size thật). */
export function detectSizeType(sizes) {
  const list = (sizes || []).map((s) => String(s).trim().toUpperCase());
  if (list.some((s) => /^\d{2}(\.\d)?$/.test(s))) return "shoe";
  if (list.some((s) => APPAREL_ORDER.includes(s))) return "apparel";
  return "free";
}

/** Nhận loại sản phẩm từ tên danh mục — dùng khi SP chưa có size thật. */
export function detectTypeFromCategory(categoryName) {
  const n = String(categoryName || "").toLowerCase();
  if (n.includes("giày") || n.includes("giay")) return "shoe";
  if (
    n.includes("áo") ||
    n.includes("quần") ||
    n.includes("quan") ||
    n.includes("bộ") ||
    n.includes("bo đồ") ||
    n.includes("bo do")
  ) {
    return "apparel";
  }
  // Tất / balo / túi / phụ kiện → Free
  return "free";
}

/** Áo/quần: ưu tiên cân nặng, tăng 1 cỡ nếu cao & gầy. */
function recommendApparel(weight, height) {
  let idx;
  if (weight < 50) idx = 1; // S
  else if (weight < 60) idx = 2; // M
  else if (weight < 70) idx = 3; // L
  else if (weight < 80) idx = 4; // XL
  else if (weight < 95) idx = 5; // XXL
  else idx = 6; // XXXL

  // Người cao mà nhẹ cân → cộng 1 cỡ cho dáng dài
  const bmi = weight / Math.pow(height / 100, 2);
  if (height >= 178 && bmi < 21 && idx < APPAREL_ORDER.length - 1) idx += 1;

  return APPAREL_ORDER[idx];
}

/** Giày: ước lượng EU theo chiều cao + giới tính. */
function recommendShoe(height, gender) {
  let size;
  if (gender === "female") {
    if (height < 155) size = 36;
    else if (height < 160) size = 37;
    else if (height < 165) size = 38;
    else if (height < 170) size = 39;
    else if (height < 175) size = 40;
    else size = 41;
  } else {
    if (height < 160) size = 39;
    else if (height < 165) size = 40;
    else if (height < 170) size = 41;
    else if (height < 175) size = 42;
    else if (height < 180) size = 43;
    else if (height < 185) size = 44;
    else size = 45;
  }
  return String(size);
}

/** Tìm size sẵn có gần nhất với gợi ý. */
function matchAvailable(recommended, sizes, type) {
  const available = (sizes || []).map((s) => String(s).trim());
  if (!available.length) return null;
  if (available.some((s) => s.toUpperCase() === recommended.toUpperCase())) {
    return available.find((s) => s.toUpperCase() === recommended.toUpperCase());
  }

  if (type === "shoe") {
    const target = parseFloat(recommended);
    let best = null;
    let bestDiff = Infinity;
    for (const s of available) {
      const n = parseFloat(s);
      if (!Number.isFinite(n)) continue;
      const diff = Math.abs(n - target);
      if (diff < bestDiff) {
        bestDiff = diff;
        best = s;
      }
    }
    return best;
  }

  if (type === "apparel") {
    const target = APPAREL_ORDER.indexOf(recommended.toUpperCase());
    let best = null;
    let bestDiff = Infinity;
    for (const s of available) {
      const i = APPAREL_ORDER.indexOf(s.toUpperCase());
      if (i < 0) continue;
      const diff = Math.abs(i - target);
      if (diff < bestDiff) {
        bestDiff = diff;
        best = s;
      }
    }
    return best;
  }

  return available[0];
}

/**
 * @returns {{ recommended: string, matched: string|null, type: string, note: string }}
 */
export function adviseSize({ gender, height, weight, sizes, categoryName }) {
  // Ưu tiên size thật của SP; nếu SP chưa có size (toàn "Default") → dựa vào danh mục.
  const sizeType = detectSizeType(sizes);
  const hasRealSizes = (sizes || []).length > 0;
  const type = hasRealSizes ? sizeType : detectTypeFromCategory(categoryName);

  if (type === "free") {
    return {
      recommended: "Free",
      matched: null,
      type,
      note: "Sản phẩm dạng Free size — một cỡ phù hợp đa số vóc dáng, không cần chọn size.",
    };
  }

  const recommended =
    type === "shoe" ? recommendShoe(height, gender) : recommendApparel(weight, height);
  const matched = hasRealSizes ? matchAvailable(recommended, sizes, type) : null;

  let note;
  if (type === "shoe") {
    note = "Gợi ý size giày (EU) mang tính tham khảo theo chiều cao & giới tính.";
  } else {
    note = "Gợi ý size áo/quần dựa trên cân nặng & chiều cao.";
  }
  if (matched && matched.toUpperCase() !== recommended.toUpperCase()) {
    note += " Sản phẩm không có đúng size gợi ý — đã chọn size gần nhất.";
  } else if (!hasRealSizes) {
    note += " Đây là gợi ý chung để bạn tham khảo khi đặt hàng.";
  }

  return { recommended, matched, type, note };
}
