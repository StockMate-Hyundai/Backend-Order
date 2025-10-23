package com.stockmate.order.common.config.webSocket;

import com.stockmate.order.api.websocket.handler.OrderWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final OrderWebSocketHandler orderWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 순수 WebSocket (SockJS 없이)
        registry.addHandler(orderWebSocketHandler, "/ws/order")
                .setAllowedOrigins("*")
                .setAllowedOriginPatterns("*");
        
        // SockJS 지원 (브라우저 호환성)
        registry.addHandler(orderWebSocketHandler, "/ws/order/sockjs")
                .setAllowedOrigins("*")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setHeartbeatTime(25000)
                .setDisconnectDelay(5000)
                .setSessionCookieNeeded(false);
    }
}
