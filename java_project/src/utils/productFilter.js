/** Helpers validate / parse / build query cho guest product search & filter */

export const KEYWORD_MAX = 100;
export const PRICE_MAX = 100_000_000;

/**
 * @returns {{ ok: boolean, errors: Record<string, string>, values: { keyword: string, min: number|null, max: number|null } }}
 */
export function validateProductFilters({ keyword = "", min = null, max = null } = {}) {
  const errors = {};
  const trimmed = String(keyword || "").trim();

  if (trimmed.length > KEYWORD_MAX) {
    errors.keyword = `Từ khóa tối đa ${KEYWORD_MAX} ký tự`;
  }

  let safeMin = null;
  let safeMax = null;

  if (min !== null && min !== "" && Number.isFinite(Number(min))) {
    safeMin = Number(min);
    if (safeMin < 0) errors.min = "Giá từ không được âm";
    if (safeMin > PRICE_MAX) errors.min = "Giá từ vượt quá giới hạn";
  } else if (min !== null && min !== "" && !Number.isFinite(Number(min))) {
    errors.min = "Giá từ không hợp lệ";
  }

  if (max !== null && max !== "" && Number.isFinite(Number(max))) {
    safeMax = Number(max);
    if (safeMax < 0) errors.max = "Giá đến không được âm";
    if (safeMax > PRICE_MAX) errors.max = "Giá đến vượt quá giới hạn";
  } else if (max !== null && max !== "" && !Number.isFinite(Number(max))) {
    errors.max = "Giá đến không hợp lệ";
  }

  if (safeMin != null && safeMax != null && safeMin > safeMax) {
    errors.max = "Giá đến phải lớn hơn hoặc bằng giá từ";
  }

  return {
    ok: Object.keys(errors).length === 0,
    errors,
    values: {
      keyword: trimmed,
      min: safeMin,
      max: safeMax,
    },
  };
}

export function buildProductQueryParams({
  page = 0,
  sort = "createDate",
  dir = "desc",
  cat = "",
  keyword = "",
  min = null,
  max = null,
} = {}) {
  const params = {
    page: Number(page) || 0,
    sort: sort || "createDate",
    dir: dir === "asc" ? "asc" : "desc",
  };
  if (cat) params.cat = cat;
  if (keyword) params.keyword = keyword;
  if (min != null) params.min = min;
  if (max != null) params.max = max;
  return params;
}

export function filtersToRouteQuery(filters) {
  const query = {};
  if (filters.keyword?.trim()) query.keyword = filters.keyword.trim();
  if (filters.cat) query.cat = filters.cat;
  if (Number.isFinite(filters.min)) query.min = String(filters.min);
  if (Number.isFinite(filters.max)) query.max = String(filters.max);
  if (filters.sort && filters.sort !== "createDate") query.sort = filters.sort;
  if (filters.dir && filters.dir !== "desc") query.dir = filters.dir;
  if (filters.page > 0) query.page = String(filters.page);
  return query;
}

function parseOptionalPrice(value) {
  if (value === "" || value == null) return null;
  const n = Number(value);
  return Number.isFinite(n) ? n : null;
}

export function applyRouteQueryToFilters(query, filters) {
  if (query.keyword != null) filters.keyword = String(query.keyword);
  if (query.cat != null) filters.cat = String(query.cat);

  if (Object.prototype.hasOwnProperty.call(query, "min")) {
    filters.min = parseOptionalPrice(query.min);
  }
  if (Object.prototype.hasOwnProperty.call(query, "max")) {
    filters.max = parseOptionalPrice(query.max);
  }

  if (query.sort) filters.sort = String(query.sort);
  if (query.dir) filters.dir = String(query.dir);

  if (query.page != null && query.page !== "") {
    const p = Number(query.page);
    filters.page = Number.isFinite(p) && p >= 0 ? Math.floor(p) : 0;
  } else {
    filters.page = 0;
  }
}
