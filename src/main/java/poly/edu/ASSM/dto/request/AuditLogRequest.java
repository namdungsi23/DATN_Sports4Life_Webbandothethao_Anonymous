package poly.edu.ASSM.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuditLogRequest {

    private Integer id;

    @Size(max = 50, message = "Action tối đa 50 ký tự")
    private String action;

    @Size(max = 100, message = "TableName tối đa 100 ký tự")
    private String tableName;

    private Integer recordId;
    private String oldData;
    private String newData;
}
