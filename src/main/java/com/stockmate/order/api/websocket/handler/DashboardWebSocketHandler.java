package com.stockmate.order.api.websocket.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmate.order.api.websocket.dto.DashboardNotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardWebSocketHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper;
    
    // 세션 관리
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    // 타입별 세션 관리 (admin, warehouse)
    private final Map<String, Set<String>> typeToSessions = new ConcurrentHashMap<>(); // "admin" → Set<SessionId>
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("✅ 대시보드 WebSocket 연결 수립 성공 - Session ID: {}, Remote Address: {}, URI: {}", 
                session.getId(), session.getRemoteAddress(), session.getUri());
        
        try {
            String type = extractType(session);
            if (type == null || (!type.equals("admin") && !type.equals("warehouse"))) {
                log.warn("유효하지 않은 type 파라미터 - Session ID: {}, Type: {}", session.getId(), type);
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("유효하지 않은 type 파라미터 (admin 또는 warehouse만 허용)"));
                return;
            }
            
            // 세션 등록
            sessions.put(session.getId(), session);
            typeToSessions.computeIfAbsent(type, k -> ConcurrentHashMap.newKeySet()).add(session.getId());
            
            log.info("대시보드 WebSocket 세션 등록 완료 - Session ID: {}, Type: {}, 활성 세션 수: {}", 
                    session.getId(), type, typeToSessions.get(type).size());
        } catch (Exception e) {
            log.error("대시보드 WebSocket 연결 중 오류 발생 - Session ID: {}, Error: {}", session.getId(), e.getMessage(), e);
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("연결 오류"));
            return;
        }
    }

    /**
     * URI에서 type 파라미터 추출
     */
    private String extractType(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            String type = UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("type");
            return type;
        } catch (Exception e) {
            log.error("type 파라미터 추출 중 오류 발생 - Session ID: {}, Error: {}", session.getId(), e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String payload = (String) message.getPayload();
        log.info("대시보드 WebSocket 메시지 수신 - Session ID: {}, Payload: {}", session.getId(), payload);
        // 대시보드 WebSocket은 단방향 통신 (서버 → 클라이언트)만 지원
    }

    /**
     * 관리자에게 새 주문 알림 전송
     */
    public void notifyAdminNewOrder(Long orderId, String orderNumber) {
        log.info("관리자에게 새 주문 알림 전송 - Order ID: {}, Order Number: {}", orderId, orderNumber);
        
        Set<String> adminSessions = typeToSessions.get("admin");
        if (adminSessions == null || adminSessions.isEmpty()) {
            log.warn("연결된 관리자 세션이 없음 - Order ID: {}", orderId);
            return;
        }
        
        String message = orderNumber + " 신규 주문이 발생하였습니다.";
        DashboardNotificationResponse notification = DashboardNotificationResponse.builder()
                .type("DASHBOARD_NOTIFICATION")
                .message(message)
                .data(DashboardNotificationResponse.DashboardData.builder()
                        .orderId(orderId)
                        .orderNumber(orderNumber)
                        .build())
                .build();
        
        int sentCount = 0;
        for (String sessionId : adminSessions) {
            WebSocketSession session = sessions.get(sessionId);
            if (session != null && session.isOpen()) {
                sendMessage(session, notification);
                sentCount++;
            } else {
                // 연결이 끊어진 세션 제거
                adminSessions.remove(sessionId);
                sessions.remove(sessionId);
            }
        }
        
        log.info("관리자 알림 전송 완료 - Order ID: {}, Order Number: {}, 전송된 세션 수: {}", 
                orderId, orderNumber, sentCount);
    }

    /**
     * 창고관리자에게 주문 승인 알림 전송
     */
    public void notifyWarehouseOrderApproved(Long orderId, String orderNumber) {
        log.info("창고관리자에게 주문 승인 알림 전송 - Order ID: {}, Order Number: {}", orderId, orderNumber);
        
        Set<String> warehouseSessions = typeToSessions.get("warehouse");
        if (warehouseSessions == null || warehouseSessions.isEmpty()) {
            log.warn("연결된 창고관리자 세션이 없음 - Order ID: {}", orderId);
            return;
        }
        
        String message = orderNumber + " 신규 주문 승인이 발생하였습니다.";
        DashboardNotificationResponse notification = DashboardNotificationResponse.builder()
                .type("DASHBOARD_NOTIFICATION")
                .message(message)
                .data(DashboardNotificationResponse.DashboardData.builder()
                        .orderId(orderId)
                        .orderNumber(orderNumber)
                        .build())
                .build();
        
        int sentCount = 0;
        for (String sessionId : warehouseSessions) {
            WebSocketSession session = sessions.get(sessionId);
            if (session != null && session.isOpen()) {
                sendMessage(session, notification);
                sentCount++;
            } else {
                // 연결이 끊어진 세션 제거
                warehouseSessions.remove(sessionId);
                sessions.remove(sessionId);
            }
        }
        
        log.info("창고관리자 알림 전송 완료 - Order ID: {}, Order Number: {}, 전송된 세션 수: {}", 
                orderId, orderNumber, sentCount);
    }

    private void sendMessage(WebSocketSession session, Object message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(jsonMessage));
            log.debug("대시보드 WebSocket 메시지 전송 완료 - Session ID: {}, Message: {}", session.getId(), jsonMessage);
        } catch (Exception e) {
            log.error("대시보드 WebSocket 메시지 전송 중 오류 발생 - Session ID: {}, Error: {}", 
                    session.getId(), e.getMessage(), e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("❌ 대시보드 WebSocket 전송 오류 발생 - Session ID: {}, Error: {}", 
                session.getId(), exception.getMessage(), exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        log.info("대시보드 WebSocket 연결 종료 - Session ID: {}, Close Status: {}", session.getId(), closeStatus);
        
        // 세션 제거
        sessions.remove(session.getId());
        
        // 타입별 세션에서 제거
        for (Map.Entry<String, Set<String>> entry : typeToSessions.entrySet()) {
            entry.getValue().remove(session.getId());
        }
        
        log.info("현재 활성 대시보드 세션 수: {}", sessions.size());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}

