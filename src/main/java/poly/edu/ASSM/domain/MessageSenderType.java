package poly.edu.ASSM.domain;

/**
 * Phía gửi tin nhắn trong Conversation.
 * SYSTEM dành cho tin hệ thống (gán nhân viên, đóng hội thoại, ...).
 */
public enum MessageSenderType {
    USER,
    EMPLOYEE,
    SYSTEM;

    public static MessageSenderType parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Loại người gửi không hợp lệ");
        }
        return MessageSenderType.valueOf(value.trim().toUpperCase());
    }
}
