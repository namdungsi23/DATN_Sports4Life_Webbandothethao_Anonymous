package poly.edu.ASSM.domain;

public enum ShippingStatus {
    PENDING("Chờ lấy hàng"),
    PICKED_UP("Đã lấy hàng"),
    IN_TRANSIT("Đang vận chuyển"),
    OUT_FOR_DELIVERY("Đang giao"),
    DELIVERED("Đã giao hàng"),
    FAILED("Giao thất bại"),
    RETURNED("Hoàn trả");

    private final String label;

    ShippingStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static ShippingStatus parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Trạng thái vận chuyển không được để trống");
        }
        try {
            return ShippingStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Trạng thái vận chuyển không hợp lệ: " + value.trim());
        }
    }
}
