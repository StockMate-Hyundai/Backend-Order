package com.stockmate.order.api.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartDetailApiResponse {
    private int status;
    private boolean success;
    private String message;
    private List<PartDetailResponseDTO> data;
}
