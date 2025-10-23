package com.stockmate.order.api.websocket.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmate.order.api.order.entity.OrderStatus;
import com.stockmate.order.api.websocket.dto.OrderApprovalResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderWebSocketHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper;
    
    // 세션 관리
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<Long, String> userToSession = new ConcurrentHashMap<>(); // 사용자 ID → 세션 ID 매핑
    private final Map<String, Long> sessionToUser = new ConcurrentHashMap<>(); // 세션 ID → 사용자 ID 매핑

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("✅ WebSocket 연결 수립 성공 - Session ID: {}, Remote Address: {}, URI: {}, Headers: {}", 
                session.getId(), session.getRemoteAddress(), session.getUri(), session.getHandshakeHeaders());
        
        // 토큰 검증 및 사용자 매핑
        try {
            Long userId = authenticateAndMapUser(session);
            if (userId != null) {
                sessions.put(session.getId(), session);
                log.info("사용자 인증 및 매핑 완료 - User ID: {}, Session ID: {}", userId, session.getId());
            } else {
                log.warn("사용자 인증 실패 - Session ID: {}", session.getId());
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("인증 실패"));
                return;
            }
        } catch (Exception e) {
            log.error("WebSocket 인증 중 오류 발생 - Session ID: {}, Error: {}", session.getId(), e.getMessage(), e);
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("인증 오류"));
            return;
        }
        
        log.info("현재 활성 세션 수: {}", sessions.size());
    }

    /**
     * 사용자 ID 추출 및 매핑
     */
    private Long authenticateAndMapUser(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            String userIdParam = UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("userId");
            
            if (userIdParam == null || userIdParam.isEmpty()) {
                log.warn("사용자 ID가 없음 - Session ID: {}", session.getId());
                return null;
            }
            
            Long userId = Long.parseLong(userIdParam);
            
            // 사용자 매핑
            registerUser(userId, session.getId());
            return userId;
            
        } catch (Exception e) {
            log.error("사용자 ID 추출 중 오류 발생 - Session ID: {}, Error: {}", session.getId(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * 사용자 ID와 WebSocket 세션을 매핑
     */
    public void registerUser(Long userId, String sessionId) {
        userToSession.put(userId, sessionId);
        sessionToUser.put(sessionId, userId);
        log.info("사용자 등록 완료 - User ID: {}, Session ID: {}", userId, sessionId);
    }


    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String payload = (String) message.getPayload();
        log.info("WebSocket 메시지 수신 - Session ID: {}, Payload: {}", session.getId(), payload);
        
        // WebSocket은 단방향 통신 (서버 → 클라이언트)만 지원
        // 주문 승인 요청은 API 호출로만 처리
        log.info("WebSocket은 응답 전용입니다. 주문 승인은 API를 통해 요청하세요.");
    }

    public void sendOrderStatusUpdate(Long orderId, OrderStatus status, String step, String message, Object data) {
        // 주문 ID로 사용자 ID를 찾아서 해당 사용자에게만 전송
        // (실제로는 주문을 생성한 사용자에게만 전송해야 함)
        OrderApprovalResponse response = OrderApprovalResponse.builder()
                .type("ORDER_APPROVAL_RESPONSE")
                .orderId(orderId)
                .status(status)
                .message(message)
                .step(step)
                .data(data)
                .build();
        
        // TODO: 주문 ID로 사용자 ID를 찾아서 해당 사용자에게만 전송
        // 현재는 모든 활성 세션에 전송 (임시)
        sessions.values().stream()
                .filter(WebSocketSession::isOpen)
                .forEach(session -> sendMessage(session, response));
        
        log.info("주문 상태 업데이트 전송 완료 - Order ID: {}, Status: {}, Step: {}, 활성 세션 수: {}", 
                orderId, status, step, sessions.size());
    }

    /**
     * 특정 사용자에게만 메시지 전송
     */
    public void sendToUser(Long userId, Long orderId, OrderStatus status, String step, String message, Object data) {
        String sessionId = userToSession.get(userId);
        if (sessionId != null) {
            WebSocketSession session = sessions.get(sessionId);
            if (session != null && session.isOpen()) {
                OrderApprovalResponse response = OrderApprovalResponse.builder()
                        .type("ORDER_APPROVAL_RESPONSE")
                        .orderId(orderId)
                        .status(status)
                        .message(message)
                        .step(step)
                        .data(data)
                        .build();
                
                sendMessage(session, response);
                log.info("사용자별 메시지 전송 완료 - User ID: {}, Order ID: {}, Status: {}, Step: {}", 
                        userId, orderId, status, step);
            } else {
                log.warn("사용자 세션이 비활성 상태 - User ID: {}, Session ID: {}", userId, sessionId);
            }
        } else {
            log.warn("사용자 세션을 찾을 수 없음 - User ID: {}", userId);
        }
    }

    private void sendMessage(WebSocketSession session, Object message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(jsonMessage));
            log.info("WebSocket 메시지 전송 완료 - Session ID: {}, Message: {}", session.getId(), jsonMessage);
        } catch (Exception e) {
            log.error("WebSocket 메시지 전송 중 오류 발생", e);
        }
    }

    private void sendError(WebSocketSession session, String errorMessage) {
        try {
            OrderApprovalResponse errorResponse = OrderApprovalResponse.builder()
                    .type("ORDER_APPROVAL_RESPONSE")
                    .orderId(null)
                    .status(OrderStatus.REJECTED)
                    .message(errorMessage)
                    .step("ERROR")
                    .build();
            
            sendMessage(session, errorResponse);
        } catch (Exception e) {
            log.error("에러 메시지 전송 중 오류 발생", e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("❌ WebSocket 전송 오류 발생 - Session ID: {}, Remote Address: {}, URI: {}, Error: {}", 
                session.getId(), session.getRemoteAddress(), session.getUri(), exception.getMessage(), exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        log.info("WebSocket 연결 종료 - Session ID: {}, Close Status: {}", session.getId(), closeStatus);
        
        // 사용자 매핑 정리
        Long userId = sessionToUser.remove(session.getId());
        if (userId != null) {
            userToSession.remove(userId);
            log.info("사용자 매핑 정리 완료 - User ID: {}, Session ID: {}", userId, session.getId());
        }
        
        // 세션 정리
        sessions.remove(session.getId());
        log.info("현재 활성 세션 수: {}", sessions.size());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
