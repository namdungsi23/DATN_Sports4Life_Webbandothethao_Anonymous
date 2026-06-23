/** Mirrors poly.edu.ASSM.security.SpringRoleNames.normalize */
export function normalizeRole(raw) {
  if (raw == null) return "";
  const s = String(raw).trim().toUpperCase();
  if (s.length === 0) return "";
  return s.startsWith("ROLE_") ? s : `ROLE_${s}`;
}

export function hasAdminRole(roles) {
  if (!Array.isArray(roles)) return false;
  return roles.map(normalizeRole).includes("ROLE_ADMIN");
}
