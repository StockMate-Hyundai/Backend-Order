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
@Schema(description = "월별 TOP 매출량/순이익 리포트 응답 DTO")
public class TopSalesResponseDTO {

    @Schema(description = "조회 기준 년도", example = "2024")
    private Integer year;

    @Schema(description = "조회 기준 월", example = "11")
    private Integer month;

    @Schema(description = "TOP 10 매출량 부품")
    private List<PartSalesData> topRevenue;

    @Schema(description = "TOP 10 순이익 부품")
    private List<PartSalesData> topProfit;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "부품 매출/순이익 데이터")
    public static class PartSalesData {

        @Schema(description = "순위 (1~10)", example = "1")
        private Integer rank;

        @Schema(description = "부품 ID", example = "1")
        private Long partId;

        @Schema(description = "부품명", example = "브레이크 패드")
        private String partName;

        @Schema(description = "카테고리명", example = "브레이크")
        private String categoryName;

        @Schema(description = "판매 수량", example = "150")
        private Long quantity;

        @Schema(description = "단가 (판매가)", example = "50000")
        private Long unitPrice;

        @Schema(description = "총 매출액 (단가 × 수량)", example = "7500000")
        private Long totalRevenue;

        @Schema(description = "총 원가", example = "5250000")
        private Long totalCost;

        @Schema(description = "순이익 (총 매출액 - 총 원가)", example = "2250000")
        private Long netProfit;
    }
}

