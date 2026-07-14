package poly.edu.ASSM.domain;

import java.util.EnumSet;
import java.util.Set;

public enum OrderStatus {
    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    SHIPPING("Đang giao"),
    DELIVERED("Đã giao"),
    CANCELLED("Đã hủy");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public Set<OrderStatus> allowedNext() {
        return switch (this) {
            case PENDING -> EnumSet.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> EnumSet.of(SHIPPING, CANCELLED);
            case SHIPPING -> EnumSet.of(DELIVERED);
            case DELIVERED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public boolean canTransitionTo(OrderStatus next) {
        return next != null && allowedNext().contains(next);
    }

    public static OrderStatus parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Trạng thái đơn hàng không được để trống");
        }
        try {
            return OrderStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Trạng thái đơn hàng không hợp lệ: " + value.trim());
        }
    }
}
