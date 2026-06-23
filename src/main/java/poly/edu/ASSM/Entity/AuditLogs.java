package poly.edu.ASSM.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "AuditLogs")
public class AuditLogs {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Integer id;

    @Column(name = "\"Action\"", length = 50)
    private String action;

    @Column(name = "TableName", length = 100)
    private String tableName;

    @Column(name = "RecordId")
    private Integer recordId;

    @Nationalized
    @Lob
    @Column(name = "OldData")
    private String oldData;

    @Nationalized
    @Lob
    @Column(name = "NewData")
    private String newData;

    @ColumnDefault("getdate()")
    @Column(name = "CreatedAt")
    private Instant createdAt;


}