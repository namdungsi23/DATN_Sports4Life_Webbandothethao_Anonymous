package poly.edu.ASSM.domain;

public enum PaymentMethod {
    CASH("Tiền mặt / COD"),
    MOMO("MoMo"),
    TECHCOMBANK("Techcombank");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static PaymentMethod parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Phương thức thanh toán không được để trống");
        }
        try {
            return PaymentMethod.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ: " + value.trim());
        }
    }

    /** COD → chưa thanh toán; chuyển khoản online → đã thanh toán */
    public PaymentStatus resolvePaymentStatus() {
        return switch (this) {
            case MOMO, TECHCOMBANK -> PaymentStatus.PAID;
            case CASH -> PaymentStatus.UNPAID;
        };
    }
}
