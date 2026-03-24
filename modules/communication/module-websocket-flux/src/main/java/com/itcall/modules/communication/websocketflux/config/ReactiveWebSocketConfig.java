package com.itcall.modules.communication.websocketflux.config;

import com.itcall.modules.communication.websocketflux.handler.ReactiveWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ReactiveWebSocketConfig {

    private final ReactiveWebSocketHandler webSocketHandler;
    private final WebSocketProperties properties;

    public ReactiveWebSocketConfig(ReactiveWebSocketHandler webSocketHandler, WebSocketProperties properties) {
        this.webSocketHandler = webSocketHandler;
        this.properties = properties;
    }

    @Bean
    public HandlerMapping webSocketHandlerMapping() {
        Map<String, WebSocketHandler> map = new HashMap<>();
        String uri = properties.getUri();
        if (uri == null || uri.isEmpty()) {
            uri = "/wss";
        }
        map.put(uri, webSocketHandler);

        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setOrder(1);
        mapping.setUrlMap(map);
        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
