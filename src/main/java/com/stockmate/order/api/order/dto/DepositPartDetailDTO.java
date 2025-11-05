package com.stockmate.order.api.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class DepositPartDetailDTO {
    private Long id;
    private String name;
    private String image;
    private String korName;
    private String categoryName;

    public static DepositPartDetailDTO of (PartDetailResponseDTO pd) {
        return DepositPartDetailDTO.builder()
                .id(pd.getId())
                .name(pd.getName())
                .image(pd.getImage())
                .korName(pd.getKorName())
                .categoryName(pd.getCategoryName())
                .build();
    }
}