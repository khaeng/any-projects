package com.itcall.modules.communication.websocketflux.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

@Slf4j
public class SimpleReactiveWebSocketClient {

    private final WebSocketClient client = new ReactorNettyWebSocketClient();

    public Mono<Void> connectWithToken(String url, String token) {
        String fullUrl = url + (token != null && !token.isEmpty() ? "?token=" + token : "");
        return execute(fullUrl, null);
    }

    public Mono<Void> connectWithSession(String url, String sessionId) {
        return execute(url, sessionId);
    }

    private Mono<Void> execute(String url, String sessionId) {
        URI uri = URI.create(url);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        if (sessionId != null && !sessionId.isEmpty()) {
            headers.add("Cookie", "JSESSIONID=" + sessionId);
        }

        return client.execute(uri, headers, session -> {
            log.info("Reactive WebSocket Client Connected: {}", uri);

            // Receive messages
            Mono<Void> inbound = session.receive()
                    .doOnNext(message -> {
                        if (message.getType() == WebSocketMessage.Type.TEXT) {
                            log.info("Reactive WebSocket Client Received: {}", message.getPayloadAsText());
                        } else if (message.getType() == WebSocketMessage.Type.PONG) {
                            log.info("Reactive WebSocket Client Received PONG");
                        }
                        // ReactorNettyWebSocketClient automatically responds to PING with PONG.
                    })
                    .then();

            // We can also send messages if needed through a sink, but for now just stay
            // connected
            return inbound;
        });
    }
}
