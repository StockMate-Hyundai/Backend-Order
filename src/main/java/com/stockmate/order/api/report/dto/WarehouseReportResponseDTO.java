package com.stockmate.order.api.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "월별 창고별 리포트 응답 DTO")
public class WarehouseReportResponseDTO {

    @Schema(description = "조회 기준 년도", example = "2024")
    private Integer year;

    @Schema(description = "조회 기준 월", example = "11")
    private Integer month;

    @Schema(description = "창고별 데이터")
    private List<WarehouseData> warehouses;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "창고별 상세 데이터")
    public static class WarehouseData {

        @Schema(description = "창고 코드 (A, B, C, D, E)", example = "A")
        private String warehouse;

        @Schema(description = "총 주문 건수", example = "150")
        private Long totalOrderCount;

        @Schema(description = "총 출고 건수", example = "120")
        private Long totalShippedCount;

        @Schema(description = "주문 건수 비율 (%)", example = "25.0")
        private Double orderPercentage;
    }
}

