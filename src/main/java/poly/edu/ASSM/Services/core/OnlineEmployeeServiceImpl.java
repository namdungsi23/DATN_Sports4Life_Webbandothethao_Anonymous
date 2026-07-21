package poly.edu.ASSM.Services.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import poly.edu.ASSM.Services.chat.assignment.EmployeeAssignmentStrategy;

@Service
public class OnlineEmployeeServiceImpl implements OnlineEmployeeService {

    /** employeeId → set of WebSocket sessionIds */
    private final ConcurrentHashMap<Long, Set<String>> onlineSessions = new ConcurrentHashMap<>();

    @Autowired
    private EmployeeAssignmentStrategy assignmentStrategy;

    @Override
    public void employeeConnected(Long employeeId, String sessionId) {
        if (employeeId == null || sessionId == null || sessionId.isBlank()) {
            return;
        }
        onlineSessions
                .computeIfAbsent(employeeId, id -> ConcurrentHashMap.newKeySet())
                .add(sessionId);
    }

    @Override
    public void employeeDisconnected(Long employeeId, String sessionId) {
        if (employeeId == null) {
            return;
        }
        onlineSessions.computeIfPresent(employeeId, (id, sessions) -> {
            sessions.remove(sessionId);
            return sessions.isEmpty() ? null : sessions;
        });
    }

    @Override
    public boolean isOnline(Long employeeId) {
        if (employeeId == null) {
            return false;
        }
        Set<String> sessions = onlineSessions.get(employeeId);
        return sessions != null && !sessions.isEmpty();
    }

    @Override
    public List<Long> getOnlineEmployeeIds() {
        return Collections.unmodifiableList(new ArrayList<>(onlineSessions.keySet()));
    }

    @Override
    public Set<String> getSessionIds(Long employeeId) {
        Set<String> sessions = onlineSessions.get(employeeId);
        if (sessions == null || sessions.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(sessions);
    }

    @Override
    public Optional<Long> getAvailableEmployeeId() {
        List<Long> online = getOnlineEmployeeIds();
        if (online.isEmpty()) {
            return Optional.empty();
        }
        return assignmentStrategy.chooseEmployee(online);
    }
}
