import { Router } from "express";
import {
  findAccountByUsername,
  loadAuthorityStrings,
} from "../services/accountsService.js";
import { matchesSpringPassword } from "../utils/password.js";
import { createAccessToken } from "../utils/jwt.js";

const r = Router();

r.post("/login/validate", async (req, res) => {
  const username = req.body?.username;
  const pwd = req.body?.pwd;
  if (!username || String(username).trim() === "") {
    return res.status(400).json({ message: "Missing username" });
  }
  const acc = await findAccountByUsername(String(username).trim());
  if (!acc) {
    return res.status(401).json({ message: "Invalid credentials" });
  }
  if (acc.activated === false) {
    return res.status(401).json({ message: "Account disabled" });
  }
  const ok = await matchesSpringPassword(pwd, acc.password);
  if (!ok) {
    return res.status(401).json({ message: "Invalid credentials" });
  }
  const roleAuthorities = await loadAuthorityStrings(acc.username);
  const accessToken = createAccessToken({
    username: acc.username,
    roles: roleAuthorities,
  });
  return res.json({
    status: 200,
    message: "Login successfully",
    username: acc.username,
    role: roleAuthorities,
    accessToken,
    refreshToken: "",
  });
});

export default r;
