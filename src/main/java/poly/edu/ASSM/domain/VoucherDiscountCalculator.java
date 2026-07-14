package poly.edu.ASSM.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.ASSM.Entity.Voucher;

public final class VoucherDiscountCalculator {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private VoucherDiscountCalculator() {
    }

    public static void validate(Voucher voucher, BigDecimal subTotal) {
        if (voucher == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã khuyến mãi không tồn tại");
        }
        if (voucher.getIsActive() == null || voucher.getIsActive() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã khuyến mãi đã bị vô hiệu hóa");
        }

        Instant now = Instant.now();
        if (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã khuyến mãi chưa có hiệu lực");
        }
        if (voucher.getExpiredAt() != null && now.isAfter(voucher.getExpiredAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã khuyến mãi đã hết hạn sử dụng");
        }

        int used = voucher.getUsedCount() != null ? voucher.getUsedCount() : 0;
        int quantity = voucher.getQuantity() != null ? voucher.getQuantity() : 0;
        if (quantity > 0 && used >= quantity) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã khuyến mãi đã hết lượt sử dụng");
        }

        BigDecimal minOrder = voucher.getMinOrderValue() != null ? voucher.getMinOrderValue() : BigDecimal.ZERO;
        BigDecimal safeSubTotal = subTotal != null ? subTotal : BigDecimal.ZERO;
        if (safeSubTotal.compareTo(minOrder) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Đơn hàng chưa đạt giá trị tối thiểu "
                            + minOrder.setScale(0, RoundingMode.HALF_UP).toPlainString() + "đ");
        }
    }

    public static VoucherDiscountResult calculate(Voucher voucher, BigDecimal subTotal, BigDecimal shippingFee) {
        validate(voucher, subTotal);

        BigDecimal safeSubTotal = subTotal != null ? subTotal : BigDecimal.ZERO;
        BigDecimal safeShipping = shippingFee != null ? shippingFee : BigDecimal.ZERO;

        if (isFreeShippingVoucher(voucher)) {
            BigDecimal shippingDiscount = safeShipping.min(
                    voucher.getDiscountAmount() != null ? voucher.getDiscountAmount() : safeShipping);
            return new VoucherDiscountResult(voucher, BigDecimal.ZERO, shippingDiscount, shippingDiscount, "FREESHIP");
        }

        if (voucher.getDiscountPercent() != null && voucher.getDiscountPercent() > 0) {
            BigDecimal raw = safeSubTotal
                    .multiply(BigDecimal.valueOf(voucher.getDiscountPercent()))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            if (voucher.getMaxDiscount() != null && raw.compareTo(voucher.getMaxDiscount()) > 0) {
                raw = voucher.getMaxDiscount();
            }
            BigDecimal discount = raw.min(safeSubTotal);
            return new VoucherDiscountResult(voucher, discount, BigDecimal.ZERO, discount, "PERCENT");
        }

        if (voucher.getDiscountAmount() != null && voucher.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discount = voucher.getDiscountAmount().min(safeSubTotal);
            return new VoucherDiscountResult(voucher, discount, BigDecimal.ZERO, discount, "FIXED");
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã khuyến mãi không hợp lệ");
    }

    private static boolean isFreeShippingVoucher(Voucher voucher) {
        if (voucher.getCode() != null && "FREESHIP".equalsIgnoreCase(voucher.getCode().trim())) {
            return true;
        }
        String name = voucher.getName() != null ? voucher.getName().toLowerCase() : "";
        return name.contains("miễn phí vận chuyển") || name.contains("freeship");
    }
}
