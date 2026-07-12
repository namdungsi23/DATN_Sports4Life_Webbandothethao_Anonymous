# 5 lớp bảo mật — Sports4Life

Tài liệu map kiến trúc defense-in-depth của đồ án với code hiện tại.

## Tổng quan

| Lớp | Tên | Mục tiêu |
|-----|-----|----------|
| 1 | Presentation (FE) | Validate form, route guard, token session, 401 logout |
| 2 | Transport / Network | CORS allowlist, security headers, CSRF stance (Bearer JWT) |
| 3 | Authentication | JWT access/refresh, BCrypt, OAuth2, rate-limit login, password policy |
| 4 | Authorization | Role ADMIN/STAFF + permission `@PreAuthorize` / `@adminAuth` |
| 5 | Data | Bean Validation, JPA parameterized, không lộ stack trace |

---

## Lớp 1 — Presentation

- `java_project/src/utils/validators.js` — required/email/phone/password (≥8, chữ+số)
- `java_project/src/router/index.js` — `requiresAuth` cần **user + accessToken**
- `java_project/src/services/http.js` + `api.js` — gắn Bearer; **401 → logout**
- Token access lưu `sessionStorage` (khó XSS persistent hơn `localStorage`)

## Lớp 2 — Transport

- `SecurityConfig` — CORS từ `app.security.cors.allowed-origins`
- `SecurityHeadersFilter` — `X-Frame-Options: DENY`, `nosniff`, `Referrer-Policy`, …
- CSRF **tắt** vì API SPA dùng `Authorization: Bearer` (stateless), không cookie session cho API
- OAuth redirect đưa token vào **hash fragment** (`/login#...`) thay vì query string

## Lớp 3 — Authentication

- `JwtService` — secret từ `app.security.jwt.secret` / env `JWT_SECRET`
- Access TTL 15 phút, refresh TTL 7 ngày (`/api/auth/refresh`)
- `LoginRateLimitFilter` + `LoginAttemptService` — tối đa 5 lần / 5 phút / IP
- `PasswordPolicy` + `/api/public/register` — BCrypt, gán `ROLE_USER`
- `register0` (tạo ADMIN + mật khẩu thô) **đã vô hiệu**
- Remember-me **tắt** (tránh lệch với STATELESS JWT)

## Lớp 4 — Authorization

- URL: `/api/admin/**` → `hasAnyRole('ADMIN','STAFF')`
- Method: `@PreAuthorize("@adminAuth.has('…')")`, `canWriteCatalog()`, `isAdmin()`
- Category create/update/delete → chỉ admin (`canWriteCatalog`)
- User save → chỉ admin (`isAdmin`)
- `/api/admin/v2` → `@PreAuthorize("hasAnyRole('ADMIN','STAFF')")`

## Lớp 5 — Data

- `@Valid` trên DTO (register, checkout, profile, address, …)
- `ApiExceptionHandler` — lỗi validate chuẩn `{ ok, message, errors }`; catch-all **không lộ stack**
- Truy vấn qua JPA / `@Param` (tránh SQL injection chuỗi nối)
- Mật khẩu chỉ lưu hash (PasswordEncoder)

---

## Cấu hình nhanh

```properties
app.security.jwt.secret=${JWT_SECRET:...}
app.security.cors.allowed-origins=http://localhost:5173,...
app.security.login.max-attempts=5
```

Production: đặt `JWT_SECRET` đủ dài (≥32 ký tự) qua biến môi trường, không commit secret thật.
