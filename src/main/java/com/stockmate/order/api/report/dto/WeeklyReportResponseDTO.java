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
@Schema(description = "주차별 리포트 응답 DTO")
public class WeeklyReportResponseDTO {

    @Schema(description = "조회 기준 년도", example = "2025")
    private Integer year;

    @Schema(description = "조회 기준 월", example = "10")
    private Integer month;

    @Schema(description = "주차별 데이터 (7주차)")
    private List<WeekData> weeks;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "주차별 상세 데이터")
    public static class WeekData {

        @Schema(description = "년도", example = "2025")
        private Integer year;

        @Schema(description = "월", example = "9")
        private Integer month;

        @Schema(description = "주차 (1~4)", example = "3")
        private Integer week;

        @Schema(description = "주차 표시 (예: '9월 3주차')", example = "9월 3주차")
        private String weekLabel;

        @Schema(description = "시작일", example = "2025-09-15")
        private String startDate;

        @Schema(description = "종료일", example = "2025-09-21")
        private String endDate;

        @Schema(description = "총 주문 건수", example = "45")
        private Long totalOrderCount;

        @Schema(description = "총 출고 건수", example = "38")
        private Long totalShippedCount;

        @Schema(description = "총 판매 수익 (매출)", example = "5000000")
        private Long totalRevenue;

        @Schema(description = "총 원가", example = "3500000")
        private Long totalCost;

        @Schema(description = "순수익 (매출 - 원가)", example = "1500000")
        private Long netProfit;
    }
}

