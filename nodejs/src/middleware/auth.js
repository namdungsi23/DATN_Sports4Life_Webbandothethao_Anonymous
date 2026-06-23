import { verifyAccessToken } from "../utils/jwt.js";
import { hasAdminRole, normalizeRole } from "../utils/springRoles.js";

function bearerToken(req) {
  const h = req.headers.authorization;
  if (!h || !h.startsWith("Bearer ")) return null;
  return h.slice(7).trim();
}

/**
 * Attaches req.auth = { username, roles: string[] } if valid Bearer JWT.
 * Does not require auth (for JwtAuthFilter-like behavior: anonymous continues).
 */
export function optionalJwt(req, res, next) {
  const token = bearerToken(req);
  if (!token) {
    return next();
  }
  try {
    const payload = verifyAccessToken(token);
    const rolesRaw = payload.roles;
    const roles = Array.isArray(rolesRaw)
      ? rolesRaw.map((r) => normalizeRole(String(r)))
      : [];
    req.auth = { username: payload.sub, roles };
  } catch {
    req.auth = null;
  }
  next();
}

/**
 * Require valid JWT with ROLE_ADMIN (Spring hasRole("ADMIN")).
 */
export function requireAdmin(req, res, next) {
  const token = bearerToken(req);
  if (!token) {
    return res.status(401).json({ message: "Unauthorized" });
  }
  try {
    const payload = verifyAccessToken(token);
    const rolesRaw = payload.roles;
    const roles = Array.isArray(rolesRaw) ? rolesRaw.map((r) => normalizeRole(String(r))) : [];
    if (!hasAdminRole(roles)) {
      return res.status(403).json({ message: "Forbidden" });
    }
    req.auth = { username: payload.sub, roles };
    next();
  } catch {
    return res.status(401).json({ message: "Unauthorized" });
  }
}
