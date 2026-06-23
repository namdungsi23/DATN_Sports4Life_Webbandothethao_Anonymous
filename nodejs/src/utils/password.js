import bcrypt from "bcrypt";

/**
 * Mirrors Spring PasswordEncoderConfig: delegating for {id}... and legacy plain as {noop}.
 * Supports {bcrypt} and {noop} / plain text without prefix.
 */
export async function matchesSpringPassword(rawPassword, encodedPassword) {
  if (encodedPassword == null || encodedPassword === "") {
    return false;
  }
  if (rawPassword == null) return false;

  const encoded = String(encodedPassword);
  if (encoded.startsWith("{")) {
    if (encoded.startsWith("{bcrypt}")) {
      const hash = encoded.slice("{bcrypt}".length);
      try {
        return await bcrypt.compare(String(rawPassword), hash);
      } catch {
        return false;
      }
    }
    if (encoded.startsWith("{noop}")) {
      return String(rawPassword) === encoded.slice("{noop}".length);
    }
    // Other {id} encodings (e.g. pbkdf2) are not implemented — use Spring for those users
    return false;
  }
  // Legacy: same as Spring delegating.matches(raw, "{noop}" + stored)
  return String(rawPassword) === encoded;
}
