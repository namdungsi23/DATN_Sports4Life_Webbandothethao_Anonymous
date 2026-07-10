package poly.edu.ASSM.domain;

/**
 * Trạng thái vòng đời của một Conversation hỗ trợ khách hàng.
 */
public enum ConversationStatus {
    OPEN,
    CLOSED,
    RESOLVED;

    public static ConversationStatus parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Trạng thái conversation không hợp lệ");
        }
        return ConversationStatus.valueOf(value.trim().toUpperCase());
    }

    public boolean isActive() {
        return this == OPEN;
    }
}
