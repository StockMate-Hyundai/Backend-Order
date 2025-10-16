package com.stockmate.order.api.order.dto;

import com.stockmate.order.api.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetailResponseDTO {
    private Long orderId;
    private String orderNumber;
    private Long memberId;
    private UserBatchResponseDTO userInfo;
    private List<OrderItemDetailDTO> orderItems;
    private String etc;
    private String rejectedMessage;
    private String carrier;
    private String trackingNumber;
    private LocalDate requestedShippingDate;
    private LocalDate shippingDate;
    private int totalPrice;
    private OrderStatus orderStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
