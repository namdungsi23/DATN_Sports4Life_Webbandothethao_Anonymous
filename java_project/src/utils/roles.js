/** Spring-style role names */
export const ROLE_ADMIN = "ROLE_ADMIN";
export const ROLE_STAFF = "ROLE_STAFF";
export const ROLE_USER = "ROLE_USER";

/**
 * Normalize roles from persisted user (backend may send `role` or `roles`, string or array).
 * @param {Record<string, unknown> | null | undefined} user
 * @returns {string[]}
 */
export function normalizeUserRoles(user) {
  if (!user) return [];
  const raw = user.roles ?? user.role;
  if (raw == null) return [];
  if (Array.isArray(raw)) {
    return raw.map((r) =>
      typeof r === "string" ? r : r?.authority ?? r?.name ?? String(r)
    );
  }
  if (typeof raw === "string") return [raw];
  return [];
}

/**
 * @param {Record<string, unknown> | null | undefined} user
 * @param {string[]} requiredAny — user must have at least one
 */
export function userHasAnyRole(user, requiredAny) {
  if (!requiredAny?.length) return true;
  const mine = new Set(normalizeUserRoles(user));
  return requiredAny.some((r) => mine.has(r));
}

export function userIsAdmin(user) {
  return userHasAnyRole(user, [ROLE_ADMIN, "ROLE_SUPER_ADMIN"]);
}

export function userIsStaff(user) {
  return userHasAnyRole(user, [ROLE_STAFF]);
}
