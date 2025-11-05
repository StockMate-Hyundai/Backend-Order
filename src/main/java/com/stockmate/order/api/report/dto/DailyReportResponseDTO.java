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
@Schema(description = "일자별 리포트 응답 DTO")
public class DailyReportResponseDTO {

    @Schema(description = "조회 기준 년도", example = "2024")
    private Integer year;

    @Schema(description = "조회 기준 월", example = "11")
    private Integer month;

    @Schema(description = "일자별 데이터")
    private List<DayData> days;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "일자별 상세 데이터")
    public static class DayData {

        @Schema(description = "년도", example = "2024")
        private Integer year;

        @Schema(description = "월", example = "11")
        private Integer month;

        @Schema(description = "일", example = "15")
        private Integer day;

        @Schema(description = "날짜 (YYYY-MM-DD)", example = "2024-11-15")
        private String date;

        @Schema(description = "요일 (1=월요일, 7=일요일)", example = "5")
        private Integer dayOfWeek;

        @Schema(description = "요일명", example = "금요일")
        private String dayOfWeekName;

        @Schema(description = "총 주문 건수", example = "25")
        private Long totalOrderCount;

        @Schema(description = "총 출고 건수", example = "20")
        private Long totalShippedCount;
    }
}

