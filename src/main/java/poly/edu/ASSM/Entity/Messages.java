package poly.edu.ASSM.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;
import poly.edu.ASSM.domain.MessageSenderType;
import poly.edu.ASSM.domain.MessageType;

import java.time.Instant;

/**
 * Tin nhắn thuộc một Conversation.
 * {@code senderId} = AccountId của Users hoặc Employees (cùng PK với Accounts).
 */
@Getter
@Setter
@Entity
@Table(
        name = "Messages",
        indexes = {
                @Index(name = "IX_Messages_Conversation_CreatedAt", columnList = "ConversationId, CreatedAt")
        }
)
public class Messages {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ConversationId", nullable = false)
    private Conversations conversation;

    @Enumerated(EnumType.STRING)
    @Column(name = "SenderType", nullable = false, length = 20)
    private MessageSenderType senderType;

    /**
     * AccountId của người gửi. Null khi senderType = SYSTEM.
     */
    @Column(name = "SenderId")
    private Long senderId;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'TEXT'")
    @Column(name = "MessageType", nullable = false, length = 20)
    private MessageType messageType = MessageType.TEXT;

    @Nationalized
    @Column(name = "Content", nullable = false, length = 4000)
    private String content;

    @ColumnDefault("getdate()")
    @Column(name = "CreatedAt", nullable = false)
    private Instant createdAt;

    @ColumnDefault("0")
    @Column(name = "Seen", nullable = false)
    private Boolean seen = false;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (messageType == null) {
            messageType = MessageType.TEXT;
        }
        if (seen == null) {
            seen = false;
        }
    }
}
