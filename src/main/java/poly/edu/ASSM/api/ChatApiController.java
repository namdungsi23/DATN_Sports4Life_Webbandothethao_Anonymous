package poly.edu.ASSM.api;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import poly.edu.ASSM.Services.core.ChatRealtimePublisher;
import poly.edu.ASSM.Services.core.ConversationService;
import poly.edu.ASSM.Services.core.MessageService;
import poly.edu.ASSM.domain.MessageSenderType;
import poly.edu.ASSM.dto.request.SendMessageRequest;
import poly.edu.ASSM.dto.response.ConversationDetailResponse;
import poly.edu.ASSM.dto.response.CreateConversationResponse;
import poly.edu.ASSM.dto.response.MessageResponse;

@RestController
@RequestMapping("/api/chat")
@Validated
public class ChatApiController {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ChatRealtimePublisher chatRealtimePublisher;

    /** User mở widget chat — tái sử dụng OPEN hoặc tạo mới. */
    @PostMapping("/conversations/open")
    public CreateConversationResponse open(Principal principal) {
        return conversationService.openOrCreate(principal.getName());
    }

    /** Conversation OPEN hiện tại (không tạo mới). */
    @GetMapping("/conversations/current")
    public CreateConversationResponse current(Principal principal) {
        return conversationService.getCurrentOpen(principal.getName());
    }

    /** Chi tiết + lịch sử tin nhắn. */
    @GetMapping("/conversations/{id}")
    public ConversationDetailResponse detail(Principal principal, @PathVariable("id") Long id) {
        return conversationService.getDetailForUser(principal.getName(), id);
    }

    /** Chỉ lấy lịch sử tin nhắn. */
    @GetMapping("/conversations/{id}/messages")
    public List<MessageResponse> history(Principal principal, @PathVariable("id") Long id) {
        return messageService.loadHistoryForUser(principal.getName(), id);
    }

    /** Gửi tin nhắn qua REST (fallback / khi WS chưa sẵn sàng). */
    @PostMapping("/messages")
    public MessageResponse send(Principal principal, @Valid @RequestBody SendMessageRequest request) {
        MessageResponse saved = messageService.saveUserMessage(
                principal.getName(),
                request.getConversationId(),
                request.getContent());
        chatRealtimePublisher.publishMessage(saved);
        return saved;
    }

    @PostMapping("/conversations/{id}/seen")
    public Map<String, Object> markSeen(Principal principal, @PathVariable("id") Long id) {
        int updated = messageService.markSeen(principal.getName(), id, MessageSenderType.USER);
        return Map.of("updated", updated);
    }
}
