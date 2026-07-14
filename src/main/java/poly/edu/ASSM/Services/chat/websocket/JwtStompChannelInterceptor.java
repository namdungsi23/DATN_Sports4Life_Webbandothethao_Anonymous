package poly.edu.ASSM.Services.chat.websocket;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import poly.edu.ASSM.Services.core.ChatIdentityService;
import poly.edu.ASSM.Services.core.OnlineEmployeeService;
import poly.edu.ASSM.Services.util.JwtService;
import poly.edu.ASSM.security.SpringRoleNames;

/**
 * Xác thực JWT trên STOMP CONNECT và kiểm soát quyền SUBSCRIBE topic conversation.
 */
@Component
public class JwtStompChannelInterceptor implements ChannelInterceptor {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ChatIdentityService chatIdentityService;

    @Autowired
    private OnlineEmployeeService onlineEmployeeService;

    @Autowired
    private ConversationAccessGuard conversationAccessGuard;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (command == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(command)) {
            authenticateConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            guardSubscribe(accessor);
        }

        return message;
    }

    private void authenticateConnect(StompHeaderAccessor accessor) {
        String token = extractToken(accessor);
        if (token == null) {
            throw new IllegalArgumentException("Thiếu JWT cho WebSocket CONNECT");
        }

        Claims claims = jwtService.getBody(token);
        if (!jwtService.validate(claims)) {
            throw new IllegalArgumentException("JWT không hợp lệ hoặc đã hết hạn");
        }

        String username = claims.getSubject();
        List<String> roles = toStringList(claims.get("roles"));
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(SpringRoleNames::normalize)
                .filter(s -> !s.isEmpty())
                .map(SimpleGrantedAuthority::new)
                .toList();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(username, null, authorities);
        accessor.setUser(auth);

        if (accessor.getSessionAttributes() == null) {
            accessor.setSessionAttributes(new java.util.concurrent.ConcurrentHashMap<>());
        }
        accessor.getSessionAttributes().put("username", username);

        chatIdentityService.findEmployee(username).ifPresent(employee -> {
            onlineEmployeeService.employeeConnected(employee.getId(), accessor.getSessionId());
            accessor.getSessionAttributes().put("employeeId", employee.getId());
            accessor.getSessionAttributes().put("chatRole", "EMPLOYEE");
        });
        accessor.getSessionAttributes().putIfAbsent("chatRole", "USER");
    }

    private void guardSubscribe(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            throw new IllegalArgumentException("Chưa xác thực WebSocket");
        }
        String destination = accessor.getDestination();
        Long conversationId = parseConversationId(destination);
        if (conversationId == null) {
            return;
        }
        String username = accessor.getUser().getName();
        if (!conversationAccessGuard.canAccess(username, conversationId)) {
            throw new IllegalArgumentException("Không có quyền theo dõi cuộc hội thoại này");
        }
    }

    private static String extractToken(StompHeaderAccessor accessor) {
        String auth = accessor.getFirstNativeHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7).trim();
        }
        String accessToken = accessor.getFirstNativeHeader("access_token");
        if (accessToken != null && !accessToken.isBlank()) {
            return accessToken.trim();
        }
        // SockJS handshake query: /ws?access_token=...
        Object queryToken = accessor.getSessionAttributes() != null
                ? accessor.getSessionAttributes().get("access_token")
                : null;
        if (queryToken != null) {
            return queryToken.toString().trim();
        }
        return null;
    }

    private static Long parseConversationId(String destination) {
        if (destination == null) {
            return null;
        }
        // /topic/conversations/{id} or /topic/conversations/{id}/typing
        String prefix = "/topic/conversations/";
        if (!destination.startsWith(prefix)) {
            return null;
        }
        String rest = destination.substring(prefix.length());
        int slash = rest.indexOf('/');
        String idPart = slash >= 0 ? rest.substring(0, slash) : rest;
        try {
            return Long.parseLong(idPart);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static List<String> toStringList(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().filter(o -> o != null).map(Object::toString).toList();
    }
}
