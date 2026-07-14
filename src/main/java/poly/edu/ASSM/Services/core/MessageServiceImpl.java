package poly.edu.ASSM.Services.core;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.ASSM.Entity.Conversations;
import poly.edu.ASSM.Entity.Employees;
import poly.edu.ASSM.Entity.Messages;
import poly.edu.ASSM.Entity.Users;
import poly.edu.ASSM.Repository.ConversationRepository;
import poly.edu.ASSM.Repository.MessageRepository;
import poly.edu.ASSM.domain.ConversationStatus;
import poly.edu.ASSM.domain.MessageSenderType;
import poly.edu.ASSM.domain.MessageType;
import poly.edu.ASSM.dto.response.MessageResponse;
import poly.edu.ASSM.mapper.MessageMapper;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ChatIdentityService chatIdentityService;

    @Autowired
    private MessageMapper messageMapper;

    @Override
    @Transactional
    public MessageResponse saveUserMessage(String username, Long conversationId, String content) {
        Users user = chatIdentityService.requireUser(username);
        Conversations conversation = requireOpenConversationForUser(conversationId, user.getId());
        return persist(conversation, MessageSenderType.USER, user.getId(), content);
    }

    @Override
    @Transactional
    public MessageResponse saveEmployeeMessage(String username, Long conversationId, String content) {
        Employees employee = chatIdentityService.requireEmployee(username);
        Conversations conversation = requireOpenConversationForEmployee(conversationId, employee.getId());
        return persist(conversation, MessageSenderType.EMPLOYEE, employee.getId(), content);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> loadHistoryForUser(String username, Long conversationId) {
        Users user = chatIdentityService.requireUser(username);
        conversationRepository.findByIdAndUser_Id(conversationId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy cuộc hội thoại"));
        return messageMapper.toResponseList(
                messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> loadHistoryForEmployee(String username, Long conversationId) {
        Employees employee = chatIdentityService.requireEmployee(username);
        conversationRepository.findByIdAndEmployee_Id(conversationId, employee.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy cuộc hội thoại"));
        return messageMapper.toResponseList(
                messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId));
    }

    @Override
    @Transactional
    public int markSeen(String username, Long conversationId, MessageSenderType viewerType) {
        Conversations conversation;
        if (viewerType == MessageSenderType.USER) {
            Users user = chatIdentityService.requireUser(username);
            conversation = conversationRepository.findByIdAndUser_Id(conversationId, user.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy cuộc hội thoại"));
        } else if (viewerType == MessageSenderType.EMPLOYEE) {
            Employees employee = chatIdentityService.requireEmployee(username);
            conversation = conversationRepository.findByIdAndEmployee_Id(conversationId, employee.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy cuộc hội thoại"));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại người xem không hợp lệ");
        }

        MessageSenderType opposite = viewerType == MessageSenderType.USER
                ? MessageSenderType.EMPLOYEE
                : MessageSenderType.USER;

        List<Messages> messages = messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversation.getId());
        int updated = 0;
        for (Messages message : messages) {
            if (message.getSenderType() == opposite && Boolean.FALSE.equals(message.getSeen())) {
                message.setSeen(true);
                updated++;
            }
        }
        if (updated > 0) {
            messageRepository.saveAll(messages);
        }
        return updated;
    }

    private MessageResponse persist(
            Conversations conversation,
            MessageSenderType senderType,
            Long senderId,
            String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nội dung tin nhắn không được để trống");
        }
        if (trimmed.length() > 4000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nội dung tin nhắn tối đa 4000 ký tự");
        }

        Messages message = new Messages();
        message.setConversation(conversation);
        message.setSenderType(senderType);
        message.setSenderId(senderId);
        message.setMessageType(MessageType.TEXT);
        message.setContent(trimmed);
        message.setCreatedAt(Instant.now());
        message.setSeen(false);

        Messages saved = messageRepository.save(message);

        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);

        return messageMapper.toResponse(saved);
    }

    private Conversations requireOpenConversationForUser(Long conversationId, Long userId) {
        Conversations conversation = conversationRepository.findByIdAndUser_Id(conversationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy cuộc hội thoại"));
        ensureOpen(conversation);
        return conversation;
    }

    private Conversations requireOpenConversationForEmployee(Long conversationId, Long employeeId) {
        Conversations conversation = conversationRepository.findByIdAndEmployee_Id(conversationId, employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy cuộc hội thoại"));
        ensureOpen(conversation);
        return conversation;
    }

    private void ensureOpen(Conversations conversation) {
        if (conversation.getStatus() != ConversationStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cuộc hội thoại đã kết thúc");
        }
    }
}
