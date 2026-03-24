package com.itcall.modules.communication.websocket.handler;

import com.itcall.modules.communication.websocket.config.WebSocketProperties;
import com.itcall.modules.communication.websocket.service.WebSocketServiceInf;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CustomWebSocketHandler extends TextWebSocketHandler implements WebSocketServiceInf {

    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final Map<String, Integer> missedHeartbeats = new ConcurrentHashMap<>();
    private final WebSocketProperties properties;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public CustomWebSocketHandler(WebSocketProperties properties) {
        this.properties = properties;
        startHeartbeatChecker();
    }

    private void startHeartbeatChecker() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                long timeout = properties.getHeartbeat().getTimeoutThreshold();
                userSessions.forEach((username, session) -> {
                    if (session.isOpen()) {
                        int missed = missedHeartbeats.getOrDefault(username, 0);
                        if (missed >= timeout) {
                            try {
                                session.close(CloseStatus.GOING_AWAY.withReason("Heartbeat timeout"));
                            } catch (IOException e) {
                                log.error("Error closing session for user: {}", username, e);
                            }
                        } else {
                            try {
                                session.sendMessage(new PingMessage());
                                missedHeartbeats.put(username, missed + 1);
                            } catch (IOException e) {
                                log.error("Error sending PING to user: {}", username, e);
                            }
                        }
                    }
                });
            } catch (Exception e) {
                log.error("Heartbeat checker error", e);
            }
        }, properties.getHeartbeat().getIntervalSeconds(), properties.getHeartbeat().getIntervalSeconds(),
                TimeUnit.SECONDS);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String username = getUsername(session);
        if (username != null) {
            userSessions.put(username, session);
            missedHeartbeats.put(username, 0);
            log.info("WebSocket Connected: {}", username);
        } else {
            session.close(CloseStatus.BAD_DATA.withReason("Username not found"));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String username = getUsername(session);
        if (username != null) {
            log.info("[WS] WebSocket TextMessage Received: {}", message.getPayload());
        }
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
        String username = getUsername(session);
        if (username != null) {
            missedHeartbeats.put(username, 0);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String username = getUsername(session);
        if (username != null) {
            userSessions.remove(username);
            missedHeartbeats.remove(username);
            log.info("WebSocket Disconnected: {}", username);
        }
    }

    private String getUsername(WebSocketSession session) {
        Object usernameObj = session.getAttributes().get("username");
        return usernameObj != null ? usernameObj.toString() : null;
    }

    @Override
    public void sendMessageToUser(String username, String message) throws IOException {
        WebSocketSession session = userSessions.get(username);
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(message));
        }
    }

    @Override
    public void broadcastMessage(String message) throws IOException {
        for (WebSocketSession session : userSessions.values()) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        }
    }
}
