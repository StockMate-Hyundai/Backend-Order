package com.stockmate.order.api.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "월별 리포트 응답 DTO")
public class MonthlyReportResponseDTO {

    @Schema(description = "년도", example = "2024")
    private Integer year;

    @Schema(description = "월", example = "11")
    private Integer month;

    @Schema(description = "총 주문 건수", example = "150")
    private Long totalOrderCount;

    @Schema(description = "총 출고 건수 (배송 완료 + 입고 대기 + 입고 완료)", example = "120")
    private Long totalShippedCount;

    @Schema(description = "총 주문 부품 수량", example = "350")
    private Long totalOrderItemCount;

    @Schema(description = "총 출고 부품 수량", example = "280")
    private Long totalShippedItemCount;

    @Schema(description = "총 판매 수익 (매출)", example = "15000000")
    private Long totalRevenue;

    @Schema(description = "총 원가", example = "10000000")
    private Long totalCost;

    @Schema(description = "순수익 (매출 - 원가)", example = "5000000")
    private Long netProfit;
}

