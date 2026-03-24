package com.itcall.modules.communication.websocket.client;

import java.util.concurrent.CompletableFuture;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ExecutionException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
public class SimpleJavaWebSocketClient {

    public WebSocketSession connectWithToken(String url, String token) {
        return execute(url, token, null);
    }

    public WebSocketSession connectWithSession(String url, String sessionId) {
        return execute(url, null, sessionId);
    }

    private WebSocketSession execute(String url, String token, String sessionId) {
        StandardWebSocketClient client = new StandardWebSocketClient();
        try {
            String fullUrl = url;
            if (token != null && !token.isEmpty()) {
                fullUrl += "?token=" + token;
            }

            org.springframework.web.socket.WebSocketHttpHeaders headers = new org.springframework.web.socket.WebSocketHttpHeaders();
            if (sessionId != null && !sessionId.isEmpty()) {
                headers.add("Cookie", "JSESSIONID=" + sessionId);
            }

            WebSocketHandler handler = new TextWebSocketHandler() {
                @Override
                public void afterConnectionEstablished(WebSocketSession session) {
                    log.info("Java WebSocket Client Connected: {}", session.getUri());
                }

                @Override
                protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                    log.info("Java WebSocket Client Received: {}", message.getPayload());
                }

                @Override
                protected void handlePongMessage(WebSocketSession session, PongMessage message) {
                    log.info("Java WebSocket Client Received PONG");
                }
            };

            CompletableFuture<WebSocketSession> future = client.execute(handler, headers,
                    java.net.URI.create(fullUrl));
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to connect to WebSocket", e);
            throw new RuntimeException("Failed to connect to WebSocket", e);
        }
    }
}
