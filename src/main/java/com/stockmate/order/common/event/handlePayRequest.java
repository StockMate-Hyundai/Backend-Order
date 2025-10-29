package com.stockmate.order.common.event;

import com.stockmate.order.api.order.dto.PayRequestEvent;
import com.stockmate.order.api.order.dto.PayResponseEvent;
import com.stockmate.order.api.order.entity.Order;
import com.stockmate.order.api.order.entity.OrderStatus;
import com.stockmate.order.api.order.repository.OrderRepository;
import com.stockmate.order.api.order.service.PaymentService;
import com.stockmate.order.common.exception.NotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static jakarta.transaction.Transactional.TxType.REQUIRES_NEW;

@Component
@RequiredArgsConstructor
@Slf4j
public class handlePayRequest {

    private final PaymentService paymentService;
    private final OrderRepository orderRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION)
    @Transactional(REQUIRES_NEW)

    public void handlePayRequest(PayRequestEvent event) {
        log.info("[handlePayRequest] 결제 요청 처리 시작 - orderId: {}", event.getOrderId());

        PayResponseEvent payResponse = paymentService.requestDepositPay(event);
        log.info("[handlePayRequest] 결제 응답 수신 - orderId: {}, 응답 성공 여부: {}, 응답 내용: {}",
                event.getOrderId(),
                payResponse != null ? payResponse.getIsSuccess() : null,
                payResponse);

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> {
                    log.error("[handlePayRequest] 주문 정보 없음 - orderId: {}", event.getOrderId());
                    return new NotFoundException("주문 정보를 찾을 수 없습니다.");
                });

        if (payResponse != null && Boolean.TRUE.equals(payResponse.getIsSuccess())) {
            log.info("[handlePayRequest] 결제 성공 - orderId: {}", order.getOrderId());
            order.setOrderStatus(OrderStatus.PAY_COMPLETED);
        } else {
            log.warn("[handlePayRequest] 결제 실패 - orderId: {}, 사유: {}",
                    order.getOrderId(),
                    payResponse != null ? payResponse.getEtc() : "응답 없음");
            order.setOrderStatus(OrderStatus.FAILED);
            order.setEtc(payResponse != null ? payResponse.getEtc() : "응답 없음");
        }

        orderRepository.save(order);
        log.info("[handlePayRequest] 주문 상태 저장 완료 - orderId: {}, 최종 상태: {}",
                order.getOrderId(),
                order.getOrderStatus());
    }
}