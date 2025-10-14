package com.stockmate.order.api.order.service;

import com.stockmate.order.api.order.dto.OrderRequestDTO;
import com.stockmate.order.api.order.entity.Order;
import com.stockmate.order.api.order.entity.OrderStatus;
import com.stockmate.order.api.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    @Transactional
    public void makeOrder(OrderRequestDTO orderRequestDTO, Long memberId) {
        log.info("부품 발주 시작 - Member ID: {}, Part ID: {}, Amount: {}", 
                memberId, orderRequestDTO.getPartsId(), orderRequestDTO.getAmount());

        // 부품 재고 체크
        inventoryService.checkInventory(orderRequestDTO.getPartsId(), orderRequestDTO.getAmount());

        // 주문 생성
        Order order = Order.builder()
                .memberId(memberId)
                .partId(orderRequestDTO.getPartsId())
                .amount(orderRequestDTO.getAmount())
                .etc(orderRequestDTO.getEtc())
                .orderStatus(OrderStatus.ORDER_COMPLETED)
                .requestedShippingDate(orderRequestDTO.getRequestedShippingDate())
                .rejectedMessage(null)
                .carrier(null)
                .trackingNumber(null)
                .shippingDate(null)
                .build();

        Order savedOrder = orderRepository.save(order);

        log.info("부품 발주 완료 - Order ID: {}, Member ID: {}, Part ID: {}, Amount: {}, Status: {}", 
                savedOrder.getOrderId(), savedOrder.getMemberId(), savedOrder.getPartId(), 
                savedOrder.getAmount(), savedOrder.getOrderStatus());
    }
}
