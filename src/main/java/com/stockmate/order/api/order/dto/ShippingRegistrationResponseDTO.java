package com.stockmate.order.api.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingRegistrationResponseDTO {
    private Long orderId;
    private String orderNumber;
    private String carrier;
    private String trackingNumber;
    private LocalDate shippingDate;
}
