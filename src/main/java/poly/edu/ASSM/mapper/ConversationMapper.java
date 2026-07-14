package poly.edu.ASSM.mapper;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.Conversations;
import poly.edu.ASSM.Entity.Employees;
import poly.edu.ASSM.Entity.Messages;
import poly.edu.ASSM.Entity.Users;
import poly.edu.ASSM.dto.response.ConversationDetailResponse;
import poly.edu.ASSM.dto.response.ConversationSummaryResponse;
import poly.edu.ASSM.dto.response.CreateConversationResponse;
import poly.edu.ASSM.dto.response.MessageResponse;

@Component
public class ConversationMapper {

    @Autowired
    private MessageMapper messageMapper;

    public CreateConversationResponse toCreateResponse(Conversations entity, boolean newlyCreated) {
        if (entity == null) {
            return null;
        }
        Employees employee = entity.getEmployee();
        return CreateConversationResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .employeeId(entity.getAssignedEmployeeId())
                .employeeFullName(employee != null ? employee.getFullName() : null)
                .status(entity.getStatus())
                .newlyCreated(newlyCreated)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public ConversationSummaryResponse toSummary(Conversations entity, Messages lastMessage) {
        if (entity == null) {
            return null;
        }
        Users user = entity.getUser();
        Employees employee = entity.getEmployee();
        return ConversationSummaryResponse.builder()
                .id(entity.getId())
                .userId(user != null ? user.getId() : null)
                .userFullName(user != null ? user.getFullName() : null)
                .employeeId(entity.getAssignedEmployeeId())
                .employeeFullName(employee != null ? employee.getFullName() : null)
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .lastMessagePreview(lastMessage != null ? truncate(lastMessage.getContent(), 120) : null)
                .lastMessageAt(lastMessage != null ? lastMessage.getCreatedAt() : null)
                .build();
    }

    public ConversationSummaryResponse toSummary(Conversations entity) {
        return toSummary(entity, null);
    }

    public ConversationDetailResponse toDetail(Conversations entity, List<MessageResponse> messages) {
        if (entity == null) {
            return null;
        }
        Users user = entity.getUser();
        Employees employee = entity.getEmployee();
        return ConversationDetailResponse.builder()
                .id(entity.getId())
                .userId(user != null ? user.getId() : null)
                .userFullName(user != null ? user.getFullName() : null)
                .employeeId(entity.getAssignedEmployeeId())
                .employeeFullName(employee != null ? employee.getFullName() : null)
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .messages(messages != null ? messages : List.of())
                .build();
    }

    public ConversationDetailResponse toDetail(Conversations entity, Collection<Messages> messageEntities) {
        return toDetail(entity, messageMapper.toResponseList(messageEntities));
    }

    public List<ConversationSummaryResponse> toSummaryList(Collection<Conversations> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toSummary).collect(Collectors.toList());
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "...";
    }
}
