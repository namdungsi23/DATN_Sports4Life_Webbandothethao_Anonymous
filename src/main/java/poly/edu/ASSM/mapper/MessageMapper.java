package poly.edu.ASSM.mapper;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.Messages;
import poly.edu.ASSM.dto.response.MessageResponse;

@Component
public class MessageMapper {

    public MessageResponse toResponse(Messages entity) {
        if (entity == null) {
            return null;
        }
        return MessageResponse.builder()
                .id(entity.getId())
                .conversationId(entity.getConversation() != null ? entity.getConversation().getId() : null)
                .senderType(entity.getSenderType())
                .senderId(entity.getSenderId())
                .messageType(entity.getMessageType())
                .content(entity.getContent())
                .createdAt(entity.getCreatedAt())
                .seen(entity.getSeen())
                .build();
    }

    public List<MessageResponse> toResponseList(Collection<Messages> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }
}
