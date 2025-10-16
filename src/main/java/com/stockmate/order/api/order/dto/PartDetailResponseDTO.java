package com.stockmate.order.api.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartDetailResponseDTO {
    private Long id;
    private String name;
    private int price;
    private String image;
    private String trim;
    private String model;
    private int category;
    private String korName;
    private String engName;
    private String categoryName;
    private int amount;
}
