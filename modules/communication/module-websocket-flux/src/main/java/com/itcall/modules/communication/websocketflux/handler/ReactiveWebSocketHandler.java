package com.itcall.modules.communication.websocketflux.handler;

import com.itcall.modules.communication.websocketflux.config.WebSocketProperties;
import com.itcall.modules.communication.websocketflux.service.WebSocketServiceInf;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ReactiveWebSocketHandler implements WebSocketHandler, WebSocketServiceInf {

    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final Map<String, Sinks.Many<String>> userSinks = new ConcurrentHashMap<>();
    private final WebSocketProperties properties;

    public ReactiveWebSocketHandler(WebSocketProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String username = authenticate(session);
        if (username == null) {
            return session.close();
        }

        userSessions.put(username, session);
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        userSinks.put(username, sink);

        log.info("Reactive WebSocket Connected: {}", username);

        // 1. Receive Messages stream
        Mono<Void> inbound = session.receive()
                .doOnNext(msg -> {
                    if (msg.getType() == WebSocketMessage.Type.TEXT) {
                        log.info("[WS-Flux] WebSocket TextMessage Received: {}", msg.getPayloadAsText());
                    }
                    // Browser automatically sends PONG frame in response to PING frame.
                    // WebFlux receive() includes PONG messages if the client sends them.
                })
                .then();

        // 2. Send Messages stream (from Sink)
        Flux<WebSocketMessage> outboundMessages = sink.asFlux()
                .map(session::textMessage);

        // 3. Heartbeat stream (Control Frame PING)
        Flux<WebSocketMessage> heartbeatFlux = Flux
                .interval(Duration.ofSeconds(properties.getHeartbeat().getIntervalSeconds()))
                .map(i -> session.pingMessage(factory -> factory.wrap(new byte[0])));

        // Merge outbound application messages and heartbeat PINGs
        Mono<Void> outbound = session.send(Flux.merge(outboundMessages, heartbeatFlux));

        return Mono.zip(inbound, outbound).then()
                .doFinally(sig -> {
                    userSessions.remove(username);
                    userSinks.remove(username);
                    log.info("Reactive WebSocket Disconnected: {}", username);
                });
    }

    private String authenticate(WebSocketSession session) {
        // 1. Check Query Param Token
        String query = session.getHandshakeInfo().getUri().getQuery();
        if (query != null && query.contains("token=")) {
            // Simple parse for demo
            String[] params = query.split("&");
            for (String p : params) {
                if (p.startsWith("token=")) {
                    return decodeToken(p.substring(6));
                }
            }
        }

        // 2. Check Cookie
        List<org.springframework.http.HttpCookie> cookies = session.getHandshakeInfo().getCookies().get("JSESSIONID");
        if (cookies != null && !cookies.isEmpty()) {
            return getUsernameFromSessionId(cookies.get(0).getValue());
        }

        return null; // Auth failed
    }

    private String decodeToken(String token) {
        if (token.startsWith("valid-")) {
            return token.substring(6);
        }
        return null;
    }

    private String getUsernameFromSessionId(String sessionId) {
        // Mock session lookup
        return "user-" + sessionId.hashCode();
    }

    @Override
    public Mono<Void> sendMessageToUser(String username, String message) {
        Sinks.Many<String> sink = userSinks.get(username);
        if (sink != null) {
            sink.tryEmitNext(message);
        }
        return Mono.empty();
    }

    @Override
    public Mono<Void> broadcastMessage(String message) {
        userSinks.values().forEach(sink -> sink.tryEmitNext(message));
        return Mono.empty();
    }
}
