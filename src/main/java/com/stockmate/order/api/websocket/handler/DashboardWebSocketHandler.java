package com.stockmate.order.api.websocket.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmate.order.api.notification.entity.NotificationType;
import com.stockmate.order.api.notification.service.DashboardNotificationService;
import com.stockmate.order.api.websocket.dto.DashboardNotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.socket.PingMessage;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardWebSocketHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper;
    private final DashboardNotificationService notificationService;
    
    // 세션 관리
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    // 타입별 세션 관리 (admin, warehouse)
    private final Map<String, Set<String>> typeToSessions = new ConcurrentHashMap<>(); // "admin" → Set<SessionId>
    // 세션별 마지막 활동 시간
    private final Map<String, LocalDateTime> sessionLastActivity = new ConcurrentHashMap<>();
    
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
            sessionLastActivity.put(session.getId(), LocalDateTime.now());
            
            log.info("대시보드 WebSocket 세션 등록 완료 - Session ID: {}, Type: {}, 활성 세션 수: {}", 
                    session.getId(), type, typeToSessions.get(type).size());
            
            // 참고: 읽지 않은 알림은 REST API (/api/notifications/unread)로 조회해야 합니다.
            // 웹소켓은 연결 후 발생하는 새로운 알림만 실시간으로 전송합니다.
            
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
        // Pong 메시지 처리 (클라이언트가 보낸 Pong 응답)
        if (message instanceof PongMessage) {
            sessionLastActivity.put(session.getId(), LocalDateTime.now());
            log.debug("Pong 수신 - Session ID: {}", session.getId());
            return;
        }
        
        // 일반 텍스트 메시지 (Heartbeat 등)
        if (message instanceof TextMessage) {
            String payload = (String) message.getPayload();
            // Heartbeat 메시지인 경우 활동 시간만 업데이트
            if ("heartbeat".equals(payload) || "ping".equals(payload)) {
                sessionLastActivity.put(session.getId(), LocalDateTime.now());
                log.debug("Heartbeat 수신 - Session ID: {}", session.getId());
                return;
            }
            
            log.info("대시보드 WebSocket 메시지 수신 - Session ID: {}, Payload: {}", session.getId(), payload);
            sessionLastActivity.put(session.getId(), LocalDateTime.now());
        }
        
        // 대시보드 WebSocket은 단방향 통신 (서버 → 클라이언트)만 지원
    }

    /**
     * 관리자에게 새 주문 알림 전송
     */
    public void notifyAdminNewOrder(Long orderId, String orderNumber) {
        log.info("관리자에게 새 주문 알림 전송 - Order ID: {}, Order Number: {}", orderId, orderNumber);
        
        String message = orderNumber + " 신규 주문이 발생하였습니다.";
        
        // DB에 알림 저장 (웹소켓 연결 여부와 관계없이 항상 저장)
        Long notificationId = null;
        try {
            var savedNotification = notificationService.saveNotification(NotificationType.ADMIN, message, orderId, orderNumber);
            notificationId = savedNotification.getId();
        } catch (Exception e) {
            log.error("알림 DB 저장 중 오류 발생 - Order ID: {}, Error: {}", orderId, e.getMessage(), e);
        }
        
        // 연결된 관리자 세션에 실시간 전송
        Set<String> adminSessions = typeToSessions.get("admin");
        if (adminSessions == null || adminSessions.isEmpty()) {
            log.warn("연결된 관리자 세션이 없음 - Order ID: {} (DB에는 저장됨)", orderId);
            return;
        }
        
        DashboardNotificationResponse notification = DashboardNotificationResponse.builder()
                .type("DASHBOARD_NOTIFICATION")
                .notificationId(notificationId)
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
        
        String message = orderNumber + " 신규 주문 승인이 발생하였습니다.";
        
        // DB에 알림 저장 (웹소켓 연결 여부와 관계없이 항상 저장)
        Long notificationId = null;
        try {
            var savedNotification = notificationService.saveNotification(NotificationType.WAREHOUSE, message, orderId, orderNumber);
            notificationId = savedNotification.getId();
        } catch (Exception e) {
            log.error("알림 DB 저장 중 오류 발생 - Order ID: {}, Error: {}", orderId, e.getMessage(), e);
        }
        
        // 연결된 창고관리자 세션에 실시간 전송
        Set<String> warehouseSessions = typeToSessions.get("warehouse");
        if (warehouseSessions == null || warehouseSessions.isEmpty()) {
            log.warn("연결된 창고관리자 세션이 없음 - Order ID: {} (DB에는 저장됨)", orderId);
            return;
        }
        
        DashboardNotificationResponse notification = DashboardNotificationResponse.builder()
                .type("DASHBOARD_NOTIFICATION")
                .notificationId(notificationId)
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

    /**
     * 주기적으로 모든 세션에 Ping 전송 (30초마다)
     * 연결이 살아있는지 확인하고, 죽은 세션을 정리합니다.
     * 
     * WebSocket이 자동으로 끊기는 이유:
     * 1. 게이트웨이/로드밸런서의 idle 타임아웃 (보통 60초)
     * 2. 네트워크 레벨의 TCP Keep-Alive 부재
     * 3. 프록시 서버의 연결 타임아웃
     * 
     * 해결 방법: 주기적으로 Ping을 보내서 연결을 활성 상태로 유지
     */
    @Scheduled(fixedRate = 30000) // 30초마다
    public void sendPingToAllSessions() {
        if (sessions.isEmpty()) {
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        Set<String> deadSessions = ConcurrentHashMap.newKeySet();
        
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            String sessionId = entry.getKey();
            WebSocketSession session = entry.getValue();
            
            try {
                if (session.isOpen()) {
                    // Ping 메시지 전송 (WebSocket 프로토콜의 Ping 프레임)
                    // Spring WebSocket에서 PingMessage를 사용
                    session.sendMessage(new PingMessage(ByteBuffer.wrap("keepalive".getBytes())));
                    sessionLastActivity.put(sessionId, now);
                    log.debug("Ping 전송 완료 - Session ID: {}", sessionId);
                } else {
                    log.warn("세션이 닫혀있음 - Session ID: {}", sessionId);
                    deadSessions.add(sessionId);
                }
            } catch (Exception e) {
                log.warn("Ping 전송 실패 - Session ID: {}, Error: {}", sessionId, e.getMessage());
                deadSessions.add(sessionId);
            }
        }
        
        // 죽은 세션 정리
        for (String deadSessionId : deadSessions) {
            cleanupSession(deadSessionId);
        }
        
        if (!deadSessions.isEmpty()) {
            log.info("죽은 세션 정리 완료 - 제거된 세션 수: {}, 현재 활성 세션 수: {}", 
                    deadSessions.size(), sessions.size());
        }
    }
    
    /**
     * 세션 정리
     */
    private void cleanupSession(String sessionId) {
        sessions.remove(sessionId);
        sessionLastActivity.remove(sessionId);
        
        // 타입별 세션에서 제거
        for (Map.Entry<String, Set<String>> entry : typeToSessions.entrySet()) {
            entry.getValue().remove(sessionId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        log.info("대시보드 WebSocket 연결 종료 - Session ID: {}, Close Status: {}", session.getId(), closeStatus);
        
        // 세션 정리
        cleanupSession(session.getId());
        
        log.info("현재 활성 대시보드 세션 수: {}", sessions.size());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}

