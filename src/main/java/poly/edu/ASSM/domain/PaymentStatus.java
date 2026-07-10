package poly.edu.ASSM.domain;

public enum PaymentStatus {
    UNPAID,
    PAID;

    public static PaymentStatus parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Trạng thái thanh toán không hợp lệ");
        }
        return PaymentStatus.valueOf(value.trim().toUpperCase());
    }
}
