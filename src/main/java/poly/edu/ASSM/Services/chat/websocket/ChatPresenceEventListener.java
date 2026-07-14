package poly.edu.ASSM.Services.chat.websocket;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import poly.edu.ASSM.Services.core.OnlineEmployeeService;

@Component
public class ChatPresenceEventListener {

    @Autowired
    private OnlineEmployeeService onlineEmployeeService;

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs == null) {
            return;
        }
        Object employeeId = attrs.get("employeeId");
        String sessionId = accessor.getSessionId();
        if (employeeId instanceof Long id) {
            onlineEmployeeService.employeeDisconnected(id, sessionId);
        } else if (employeeId instanceof Number number) {
            onlineEmployeeService.employeeDisconnected(number.longValue(), sessionId);
        }
    }
}
