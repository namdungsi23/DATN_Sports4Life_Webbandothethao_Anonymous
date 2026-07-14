package poly.edu.ASSM.api.admin;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import poly.edu.ASSM.Services.core.ChatRealtimePublisher;
import poly.edu.ASSM.Services.core.ConversationService;
import poly.edu.ASSM.Services.core.MessageService;
import poly.edu.ASSM.Services.core.OnlineEmployeeService;
import poly.edu.ASSM.domain.ConversationStatus;
import poly.edu.ASSM.domain.MessageSenderType;
import poly.edu.ASSM.dto.request.SendMessageRequest;
import poly.edu.ASSM.dto.response.ConversationDetailResponse;
import poly.edu.ASSM.dto.response.ConversationSummaryResponse;
import poly.edu.ASSM.dto.response.MessageResponse;

@RestController
@RequestMapping("/api/admin/chat")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@Validated
public class AdminChatApiController {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ChatRealtimePublisher chatRealtimePublisher;

    @Autowired
    private OnlineEmployeeService onlineEmployeeService;

    /** Inbox conversation của nhân viên đang đăng nhập. */
    @GetMapping("/conversations")
    public List<ConversationSummaryResponse> list(
            Principal principal,
            @RequestParam(required = false) String status) {
        ConversationStatus parsed = status == null || status.isBlank()
                ? null
                : ConversationStatus.parse(status);
        return conversationService.listForEmployee(principal.getName(), parsed);
    }

    @GetMapping("/conversations/{id}")
    public ConversationDetailResponse detail(Principal principal, @PathVariable("id") Long id) {
        return conversationService.getDetailForEmployee(principal.getName(), id);
    }

    @GetMapping("/conversations/{id}/messages")
    public List<MessageResponse> history(Principal principal, @PathVariable("id") Long id) {
        return messageService.loadHistoryForEmployee(principal.getName(), id);
    }

    @PostMapping("/messages")
    public MessageResponse send(Principal principal, @Valid @RequestBody SendMessageRequest request) {
        MessageResponse saved = messageService.saveEmployeeMessage(
                principal.getName(),
                request.getConversationId(),
                request.getContent());
        chatRealtimePublisher.publishMessage(saved);
        return saved;
    }

    @PutMapping("/conversations/{id}/status")
    public ConversationSummaryResponse updateStatus(
            Principal principal,
            @PathVariable("id") Long id,
            @RequestParam String status) {
        return conversationService.updateStatus(principal.getName(), id, ConversationStatus.parse(status));
    }

    @PostMapping("/conversations/{id}/seen")
    public Map<String, Object> markSeen(Principal principal, @PathVariable("id") Long id) {
        int updated = messageService.markSeen(principal.getName(), id, MessageSenderType.EMPLOYEE);
        return Map.of("updated", updated);
    }

    /** Debug / UI: số employee đang online. */
    @GetMapping("/online-count")
    public Map<String, Object> onlineCount() {
        return Map.of(
                "count", onlineEmployeeService.getOnlineEmployeeIds().size(),
                "employeeIds", onlineEmployeeService.getOnlineEmployeeIds());
    }
}
