package poly.edu.ASSM.domain;

/**
 * Loại nội dung tin nhắn.
 * Hiện tại chỉ hỗ trợ TEXT; IMAGE / FILE / SYSTEM để mở rộng sau.
 */
public enum MessageType {
    TEXT,
    IMAGE,
    FILE,
    SYSTEM;

    public static MessageType parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Loại tin nhắn không hợp lệ");
        }
        return MessageType.valueOf(value.trim().toUpperCase());
    }
}
