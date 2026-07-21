# Node.js mirror of the Spring Boot ASSM API

Educational project: the same JSON routes as the main Spring app, backed by the **same** SQL Server database. Only this folder is part of the mirror; Java and `java_project` sources are left unchanged.

## Prerequisites

- Node.js 18+
- A running SQL Server instance with the database and schema the Spring app already uses
- (Optional) Cloudinary account for admin product image uploads—same as Spring

## Setup

1. Copy `.env.example` to `.env` and adjust `DB_*`, `JWT_SECRET`, and optionally `CLOUDINARY_*`.
2. Install dependencies: `npm install`
3. Run: `npm start` (or `npm run dev` for watch mode; default **port 3001** so it does not conflict with Spring on 8080).

## Comparing with Spring

- Start Spring on 8080 and this server on 3001.
- Hit the same paths on both, e.g.:
  - `GET http://localhost:3001/api/public/`
  - `GET http://localhost:3001/api/public/products`
  - `POST http://localhost:3001/login/validate` (form: `username`, `pwd`, `remember-me`)
- Admin JSON routes require a Bearer token from a successful `login/validate` for an account with the `ADMIN` role.

## Optional: point the Vue app at this server (without changing committed code)

Create `java_project/.env.local` (or your environment) with something like:

```bash
VITE_API_BASE=http://localhost:3001
```

`http.js` in the Vue app uses `VITE_API_BASE` when set; the default is still the Spring port.

## Intentional gaps (parity with Spring, not the Vue `api.js` wishlist)

- **OAuth2 / Google** login is not implemented here.
- The Vue file `api.js` also calls `POST /api/public/register`, `GET /api/public/profile`, etc. Those are **not** part of the Spring `HomeAPI` (only `GET /` and `GET /products` exist there), so this mirror does not add them.

## Safety note

Do not run both backends as primary writers against production data; use for comparison and local learning only, or a disposable database.
