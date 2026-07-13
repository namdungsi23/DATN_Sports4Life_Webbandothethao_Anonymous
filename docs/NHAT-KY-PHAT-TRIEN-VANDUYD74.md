# Nhật ký phát triển — Văn Duy (vanduyd74)

> Nhánh: `vanduyd74`  
> Repo: [DATN_Sports4Life_Webbandothethao_Anonymous](https://github.com/namdungsi23/DATN_Sports4Life_Webbandothethao_Anonymous)  
> Cập nhật: 13/07/2026

Tài liệu tổng hợp các hạng mục đã triển khai / sửa lỗi trong phiên làm việc với Cursor AI.

---

## 1. Checkout & thanh toán

### Luồng người dùng
```
Giỏ hàng → /cart/checkout → /cart/payment → POST /api/checkout/confirm → (SePay) redirect cổng thanh toán
```

### Đã làm
- **UI checkout mới**: `CheckoutSteps`, `checkout.css`, `design-system.css`
- **Giỏ hàng**: lưu đúng `variantId`, size, màu (không gộp nhầm sản phẩm khác biến thể)
- **Phí ship**: 30.000đ nếu tạm tính < 499.000đ, freeship từ 499.000đ (`ShippingFeePolicy`)
- **Đơn vị vận chuyển**: chọn carrier khi checkout
- **Địa chỉ giao hàng**:
  - CRUD sổ địa chỉ (`/api/addresses`, `AddressBookView`, `CustomerAddressService`)
  - Thay thế entity cũ `UserAddress` → `Addresses`, `OrderAddresses` (snapshot khi đặt hàng)
  - Component `AddressFormFields.vue`
- **Backend checkout**: `CheckoutServiceImpl`, `CheckoutApiController`
- **Trang kết quả thanh toán**: `PaymentResultView.vue` (success / error / cancel)

### File chính
| Vai trò | File |
|---------|------|
| Frontend checkout | `java_project/src/pages/cart/CheckoutView.vue`, `PaymentView.vue` |
| API checkout | `src/main/java/poly/edu/ASSM/api/CheckoutApiController.java` |
| Service | `src/main/java/poly/edu/ASSM/Services/core/CheckoutServiceImpl.java` |
| Địa chỉ | `CustomerAddressApiController.java`, `CustomerAddressServiceImpl.java` |

---

## 2. Đăng ký / đăng nhập

### Vấn đề đã sửa
- Frontend gọi `POST /api/public/register` nhưng API **chưa tồn tại** → hiện “thành công” giả
- Google OAuth lỗi do session `STATELESS`

### Đã làm
- **`PublicAuthApiController`**: API đăng ký, hash bcrypt, tạo account + profile
- **`RegisterRequest`**: validate dữ liệu
- **`RegisterView.vue`**: hiển thị lỗi thật từ server
- **`CustomFormLoginFailureHandler`**: login sai trả JSON rõ ràng
- **`SecurityConfig`**: `SessionCreationPolicy.IF_REQUIRED` cho OAuth2
- Redirect Google: `http://localhost:8080/login/oauth2/code/google`

---

## 3. Admin — đơn hàng, sản phẩm, người dùng

### Đã làm
- **Quản lý đơn**: `AdminOrderManagementService`, cập nhật trạng thái đơn / thanh toán / vận chuyển
- **In hóa đơn**: `AdminOrderView.vue` — mẫu in, hỗ trợ `?print=1`
- **Sản phẩm admin**: biến thể, ảnh, reorder ảnh (`AdminProductCatalogService`)
- **Phân quyền**: `AdminReadOnlyNotice`, `adminAccess.js`, `AdminPermissionCodes`
- **Banner cảnh báo đơn mới**: `AdminOrderAlertBanner.vue`
- **Domain enums**: `OrderStatus`, `PaymentStatus`, `ShippingStatus`

---

## 4. Quản lý hóa đơn & báo cáo

### Đã làm
- **API báo cáo**: `/api/admin/reports/*` — summary, revenue, status-breakdown, invoices, by-user
- **Service**: `AdminReportServiceImpl`, `AdminReportApiController`
- **Frontend**: `AdminInvoiceView.vue` — KPI, biểu đồ Chart.js, lọc theo ngày/giờ (timezone VN)
- **Menu admin**: route `/admin/invoices`
- **Fix đếm hóa đơn**: mỗi đơn = 1 hóa đơn (không gộp theo user), lọc timezone VN

---

## 5. Mã khuyến mãi (Voucher)

### Đã làm
- **Backend**: `VoucherRepository`, `VoucherServiceImpl`, `VoucherDiscountCalculator`
- **Admin CRUD**: `AdminVoucherApiController`, `AdminVoucherView.vue`
- **User áp dụng khi checkout**: `POST /api/checkout/vouchers/apply`
- **Tích hợp** `CheckoutServiceImpl` — giảm tiền hàng / phí ship
- **UI**: nhập mã trong `PaymentView.vue`
- **Script DB**: `scripts/fix-vouchers-columns.sql` (chuẩn hóa `decimal(10,2)`)

---

## 6. SePay — thanh toán QR / thẻ

### Đã làm
| Thành phần | Mô tả |
|------------|--------|
| `SePayProperties` | Cấu hình merchant, env, auto-detect LIVE vs sandbox |
| `SePayServiceImpl` | Tạo form checkout, HMAC-SHA256 signature, xử lý IPN |
| `SePayIpnController` | `POST/GET /api/public/sepay/ipn` |
| `SePayPaymentApiController` | `GET /api/checkout/sepay/status/{orderId}` |
| Frontend | Option SePay trong `CheckoutView`, redirect form `utils/sepay.js`, poll trạng thái `PaymentResultView` |

### Cấu hình (local — không commit `application.properties`)
```properties
sepay.enabled=true
sepay.env=production
sepay.merchant-id=SP-LIVE-...
sepay.secret-key=spsk_live_...
sepay.checkout-url=https://pay.sepay.vn/v1/checkout/init
sepay.frontend-base-url=http://localhost:5173
app.backend.public-url=https://<ngrok-url>
```

### Lỗi đã xử lý
1. **LIVE merchant + URL sandbox** → SePay báo “chế độ thử nghiệm” → chuyển sang `pay.sepay.vn`
2. **Chữ ký sai thứ tự field** → sửa ký theo thứ tự field trong form (theo tài liệu SePay)
3. **IPN localhost không dùng được** → dùng **ngrok** public backend port 8080

### IPN trên my.sepay.vn
```
https://<ngrok-url>/api/public/sepay/ipn
```
- Xác thực: **SECRET_KEY** (cùng secret trong config)
- Test health: mở GET URL trên → `{"success":true,"service":"sepay-ipn"}`

### Chạy local khi dev SePay
1. Backend (IntelliJ) — port 8080  
2. Frontend — `npm run dev` port 5173  
3. Ngrok — `ngrok http 8080` (cần bản ≥ 3.20.0)  

**Tắt máy / restart** → chạy lại cả 3; URL ngrok đổi → cập nhật IPN trên SePay + `app.backend.public-url` + restart backend.

---

## 7. Database & migration scripts

Thư mục `scripts/`:
- `migrate-addresses-label.sql`
- `migrate-shipment-carrier.sql`
- `fix-vouchers-columns.sql`

Entity / bảng mới hoặc refactor:
- `Addresses`, `OrderAddresses`, `Carriers`, `Shipments`, `Employees`, `Positions`
- Xóa: `UserAddress`, `UserAddressMapper`, `InventoryMapper`

---

## 8. Cấu trúc API mới (tóm tắt)

| Method | Endpoint | Mô tả |
|--------|----------|--------|
| POST | `/api/public/register` | Đăng ký tài khoản |
| POST | `/api/checkout/confirm` | Xác nhận đặt hàng |
| POST | `/api/checkout/vouchers/apply` | Áp dụng voucher |
| GET | `/api/checkout/sepay/status/{orderId}` | Trạng thái thanh toán SePay |
| POST/GET | `/api/public/sepay/ipn` | IPN callback từ SePay |
| CRUD | `/api/addresses` | Sổ địa chỉ khách hàng |
| GET | `/api/admin/reports/*` | Báo cáo / hóa đơn admin |
| CRUD | `/api/admin/vouchers` | Quản lý voucher admin |

---

## 9. Lưu ý cho nhóm khi pull nhánh này

1. **`application.properties` không có trên Git** (đã gitignore) — mỗi người tự tạo file local, tham khảo mục 6.
2. **Không commit** secret key SePay, Google OAuth, Cloudinary lên GitHub.
3. Chạy migration SQL trong `scripts/` nếu DB chưa cập nhật schema.
4. Frontend: `cd java_project && npm install && npm run dev`
5. Backend: chạy Spring Boot (port 8080), SQL Server database `Sports4Life`

---

## 10. Việc có thể làm tiếp

- [ ] Deploy backend lên server public → bỏ ngrok, dùng domain thật cho IPN
- [ ] Tách `application.properties` → `application.properties.example` (không secret)
- [ ] Webhook SePay (biến động số dư) nếu cần đối soát thủ công
- [ ] Test end-to-end SePay production sau khi liên kết ngân hàng trên my.sepay.vn

---

*Tài liệu do Văn Duy tổng hợp sau các phiên pair-programming.
