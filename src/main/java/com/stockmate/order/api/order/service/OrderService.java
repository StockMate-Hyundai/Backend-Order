package com.stockmate.order.api.order.service;

import com.stockmate.order.api.order.dto.*;
import com.stockmate.order.api.order.entity.Order;
import com.stockmate.order.api.order.entity.OrderItem;
import com.stockmate.order.api.order.entity.OrderStatus;
import com.stockmate.order.api.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    @Transactional
    public void makeOrder(OrderRequestDTO orderRequestDTO, Long memberId) {
        log.info("부품 발주 시작 - Member ID: {}, 주문 항목 수: {}", 
                memberId, orderRequestDTO.getOrderItems().size());

        List<OrderItemCheckRequestDTO> checkItems = new ArrayList<>();
        for (OrderItemRequestDTO item : orderRequestDTO.getOrderItems()) {
            checkItems.add(OrderItemCheckRequestDTO.builder()
                    .partId(item.getPartId())
                    .amount(item.getAmount())
                    .build());
        }

        // 부품 재고 체크
        InventoryCheckResponseDTO checkResult = inventoryService.checkInventory(checkItems);
        
        log.info("재고 체크 완료 - 총 금액: {}", checkResult.getTotalAmount());

        // 주문 생성
        Order order = Order.builder()
                .memberId(memberId)
                .etc(orderRequestDTO.getEtc())
                .requestedShippingDate(orderRequestDTO.getRequestedShippingDate())
                .orderStatus(OrderStatus.ORDER_COMPLETED)
                .rejectedMessage(null)
                .carrier(null)
                .trackingNumber(null)
                .shippingDate(null)
                .orderItems(new ArrayList<>())
                .build();

        // 주문 항목들 생성 및 추가
        for (OrderItemRequestDTO itemRequest : orderRequestDTO.getOrderItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .partId(itemRequest.getPartId())
                    .amount(itemRequest.getAmount())
                    .build();
            
            order.getOrderItems().add(orderItem);
        }

        Order savedOrder = orderRepository.save(order);

        // 주문번호 생성 및 업데이트
        String orderNumber = "SMO-" + savedOrder.getOrderId();
        Order updatedOrder = savedOrder.toBuilder()
                .orderNumber(orderNumber)
                .build();
        orderRepository.save(updatedOrder);

        log.info("부품 발주 완료 - Order ID: {}, Order Number: {}, Member ID: {}, 주문 항목 수: {}, 총 금액: {}, Status: {}", 
                savedOrder.getOrderId(), orderNumber, savedOrder.getMemberId(), 
                savedOrder.getOrderItems().size(), checkResult.getTotalAmount(), 
                savedOrder.getOrderStatus());

        // TODO: 추후 결제 서버에 totalAmount 전송
        // paymentService.processPayment(checkResult.getTotalAmount(), savedOrder.getOrderId());
    }
}