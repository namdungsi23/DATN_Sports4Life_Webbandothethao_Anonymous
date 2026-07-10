package poly.edu.ASSM.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import poly.edu.ASSM.domain.ConversationStatus;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Hội thoại hỗ trợ giữa một User (đã đăng nhập) và một Employee phụ trách.
 * <p>
 * Assignment hiện lưu trực tiếp qua {@code employee}.
 * Service nên truy cập qua {@link #getAssignedEmployeeId()} để sau này
 * có thể thay bằng bảng ConversationAssignment mà ít đụng code nghiệp vụ.
 */
@Getter
@Setter
@Entity
@Table(
        name = "Conversations",
        indexes = {
                @Index(name = "IX_Conversations_User_Status", columnList = "UserId, Status"),
                @Index(name = "IX_Conversations_Employee_Status", columnList = "EmployeeId, Status")
        }
)
public class Conversations {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "UserId", nullable = false)
    private Users user;

    /**
     * Nhân viên đang phụ trách. Có thể null nếu chưa gán được (không có employee online).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EmployeeId")
    private Employees employee;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'OPEN'")
    @Column(name = "Status", nullable = false, length = 20)
    private ConversationStatus status = ConversationStatus.OPEN;

    @ColumnDefault("getdate()")
    @Column(name = "CreatedAt", nullable = false)
    private Instant createdAt;

    @Column(name = "UpdatedAt")
    private Instant updatedAt;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = false)
    @OrderBy("createdAt ASC")
    private Set<Messages> messages = new LinkedHashSet<>();

    /**
     * Abstraction cho assignment — thay vì đọc {@code employee} trực tiếp ở tầng service.
     * Khi chuyển sang ConversationAssignment, chỉ cần đổi implementation của method này.
     */
    public Long getAssignedEmployeeId() {
        return employee != null ? employee.getId() : null;
    }

    public boolean isAssigned() {
        return employee != null;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (status == null) {
            status = ConversationStatus.OPEN;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
