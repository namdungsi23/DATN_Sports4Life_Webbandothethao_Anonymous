package poly.edu.ASSM.domain;

public enum ShippingStatus {
    PENDING,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    FAILED,
    RETURNED;

    public static ShippingStatus parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Trạng thái vận chuyển không hợp lệ");
        }
        return ShippingStatus.valueOf(value.trim().toUpperCase());
    }
}
