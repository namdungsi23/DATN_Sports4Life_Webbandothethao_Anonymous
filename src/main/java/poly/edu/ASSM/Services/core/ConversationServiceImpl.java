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
import poly.edu.ASSM.Entity.Users;
import poly.edu.ASSM.Repository.ConversationRepository;
import poly.edu.ASSM.Repository.EmployeeRepository;
import poly.edu.ASSM.Repository.MessageRepository;
import poly.edu.ASSM.domain.ConversationStatus;
import poly.edu.ASSM.dto.response.ConversationDetailResponse;
import poly.edu.ASSM.dto.response.ConversationSummaryResponse;
import poly.edu.ASSM.dto.response.CreateConversationResponse;
import poly.edu.ASSM.mapper.ConversationMapper;

@Service
public class ConversationServiceImpl implements ConversationService {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OnlineEmployeeService onlineEmployeeService;

    @Autowired
    private ChatIdentityService chatIdentityService;

    @Autowired
    private ConversationMapper conversationMapper;

    @Override
    @Transactional
    public CreateConversationResponse openOrCreate(String username) {
        Users user = chatIdentityService.requireUser(username);

        return conversationRepository.findOpenByUserId(user.getId())
                .map(existing -> conversationMapper.toCreateResponse(existing, false))
                .orElseGet(() -> createAssigned(user));
    }

    @Override
    @Transactional(readOnly = true)
    public CreateConversationResponse getCurrentOpen(String username) {
        Users user = chatIdentityService.requireUser(username);
        Conversations conversation = conversationRepository.findOpenByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Bạn chưa có cuộc hội thoại đang mở"));
        return conversationMapper.toCreateResponse(conversation, false);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationDetailResponse getDetailForUser(String username, Long conversationId) {
        Users user = chatIdentityService.requireUser(username);
        Conversations conversation = conversationRepository.findByIdAndUser_Id(conversationId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy cuộc hội thoại"));
        return conversationMapper.toDetail(
                conversation,
                messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId));
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationDetailResponse getDetailForEmployee(String username, Long conversationId) {
        Employees employee = chatIdentityService.requireEmployee(username);
        Conversations conversation = conversationRepository
                .findByIdAndEmployee_Id(conversationId, employee.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy cuộc hội thoại"));
        return conversationMapper.toDetail(
                conversation,
                messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> listForEmployee(String username, ConversationStatus status) {
        Employees employee = chatIdentityService.requireEmployee(username);
        List<Conversations> list = status == null
                ? conversationRepository.findByEmployee_IdOrderByUpdatedAtDesc(employee.getId())
                : conversationRepository.findByEmployee_IdAndStatusOrderByUpdatedAtDesc(employee.getId(), status);
        return conversationMapper.toSummaryList(list);
    }

    @Override
    @Transactional
    public ConversationSummaryResponse updateStatus(String username, Long conversationId, ConversationStatus status) {
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái không hợp lệ");
        }
        Employees employee = chatIdentityService.requireEmployee(username);
        Conversations conversation = conversationRepository
                .findByIdAndEmployee_Id(conversationId, employee.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy cuộc hội thoại"));

        conversation.setStatus(status);
        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);
        return conversationMapper.toSummary(conversation);
    }

    private CreateConversationResponse createAssigned(Users user) {
        Long employeeId = onlineEmployeeService.getAvailableEmployeeId()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Hiện không có nhân viên hỗ trợ trực tuyến. Vui lòng thử lại sau."));

        Employees employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Hiện không có nhân viên hỗ trợ trực tuyến. Vui lòng thử lại sau."));

        Conversations conversation = new Conversations();
        conversation.setUser(user);
        conversation.setEmployee(employee);
        conversation.setStatus(ConversationStatus.OPEN);
        conversation.setCreatedAt(Instant.now());
        conversation.setUpdatedAt(Instant.now());

        Conversations saved = conversationRepository.save(conversation);
        return conversationMapper.toCreateResponse(saved, true);
    }
}
