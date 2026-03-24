package com.itcall.modules.communication.websocket.config;

import com.itcall.modules.communication.websocket.handler.CustomWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final CustomWebSocketHandler webSocketHandler;
    private final WebSocketProperties properties;

    public WebSocketConfig(CustomWebSocketHandler webSocketHandler, WebSocketProperties properties) {
        this.webSocketHandler = webSocketHandler;
        this.properties = properties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String uri = properties.getUri();
        if (uri == null || uri.isEmpty()) {
            uri = "/wss"; // Fallback default
        }

        registry.addHandler(webSocketHandler, uri)
                .addInterceptors(new AuthHandshakeInterceptor())
                .setAllowedOrigins("*");
    }

    private static class AuthHandshakeInterceptor implements HandshakeInterceptor {
        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
            if (request instanceof ServletServerHttpRequest) {
                HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

                // 1. Check Token (Query Param 'token' for now, or Authorization header if
                // accessible/forwarded)
                // Note: Standard WebSocket JS API doesn't allow custom headers easily during
                // handshake, so query param is common.
                String token = servletRequest.getParameter("token");
                if (token != null && !token.isEmpty()) {
                    String username = decodeToken(token); // Mock decoding
                    if (username != null) {
                        attributes.put("username", username);
                        return true;
                    }
                }

                // 2. Check Session
                HttpSession session = servletRequest.getSession(false);
                if (session != null) {
                    Object usernameObj = session.getAttribute("username"); // Assuming session attribute key is
                                                                           // "username"
                    if (usernameObj != null) {
                        attributes.put("username", usernameObj.toString());
                        return true;
                    }
                }
            }
            return false; // Auth fail
        }

        // Mock token decoder - integration needed later as per requirement
        private String decodeToken(String token) {
            // Logic to decode token and extract claims.
            // Returning null means invalid token.
            // Returning string means valid username.
            if (token.startsWith("valid-")) {
                return token.substring(6); // valid-user1 -> user1
            }
            return null;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                WebSocketHandler wsHandler, Exception exception) {
        }
    }
}
