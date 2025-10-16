package com.stockmate.order.api.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBatchResponseDTO {
    private Long id;
    private Long memberId;
    private String email;
    private String owner;
    private String address;
    private String storeName;
    private String businessNumber;
    private String role;
    private String verified;
    private Double latitude;
    private Double longitude;
}
