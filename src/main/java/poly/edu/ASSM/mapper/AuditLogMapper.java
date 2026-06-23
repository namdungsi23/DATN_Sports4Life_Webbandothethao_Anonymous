package poly.edu.ASSM.mapper;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.AuditLogs;
import poly.edu.ASSM.dto.request.AuditLogRequest;
import poly.edu.ASSM.dto.response.AuditLogResponse;
import poly.edu.ASSM.dto.response.PageResponse;

@Component
public class AuditLogMapper {

    public AuditLogResponse toResponse(AuditLogs entity) {
        if (entity == null) {
            return null;
        }
        return AuditLogResponse.builder()
                .id(entity.getId())
                .action(entity.getAction())
                .tableName(entity.getTableName())
                .recordId(entity.getRecordId())
                .oldData(entity.getOldData())
                .newData(entity.getNewData())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public List<AuditLogResponse> toResponseList(Collection<AuditLogs> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public AuditLogs toEntity(AuditLogRequest request) {
        AuditLogs entity = new AuditLogs();
        applyRequest(entity, request);
        return entity;
    }

    public void applyRequest(AuditLogs entity, AuditLogRequest request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setAction(request.getAction());
        entity.setTableName(request.getTableName());
        entity.setRecordId(request.getRecordId());
        entity.setOldData(request.getOldData());
        entity.setNewData(request.getNewData());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
    }

    public PageResponse<AuditLogResponse> toPageResponse(Page<AuditLogs> page) {
        return PageResponse.<AuditLogResponse>builder()
                .content(toResponseList(page.getContent()))
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .number(page.getNumber())
                .size(page.getSize())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
