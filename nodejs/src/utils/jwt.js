import jwt from "jsonwebtoken";
import { config } from "../config/index.js";
import { normalizeRole } from "./springRoles.js";

export function createAccessToken({ username, roles }) {
  const roleList = (roles || []).map((r) => normalizeRole(r)).filter((s) => s.length > 0);
  return jwt.sign(
    { roles: roleList },
    config.jwtSecret,
    {
      subject: username,
      algorithm: "HS256",
      expiresIn: config.jwtAccessExpirySeconds,
    }
  );
}

export function verifyAccessToken(token) {
  return jwt.verify(token, config.jwtSecret, { algorithms: ["HS256"] });
}
