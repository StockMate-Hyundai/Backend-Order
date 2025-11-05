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
@Schema(description = "일자별 카테고리별 판매량 리포트 응답 DTO")
public class DailyCategorySalesResponseDTO {

    @Schema(description = "조회 기준 년도", example = "2024")
    private Integer year;

    @Schema(description = "조회 기준 월", example = "11")
    private Integer month;

    @Schema(description = "일자별 카테고리별 판매량 데이터")
    private List<DayCategoryData> days;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "일자별 카테고리별 판매량 상세 데이터")
    public static class DayCategoryData {

        @Schema(description = "년도", example = "2024")
        private Integer year;

        @Schema(description = "월", example = "11")
        private Integer month;

        @Schema(description = "일", example = "1")
        private Integer day;

        @Schema(description = "날짜 (YYYY-MM-DD)", example = "2024-11-01")
        private String date;

        @Schema(description = "카테고리별 판매량")
        private List<CategorySales> categories;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "카테고리별 판매량")
    public static class CategorySales {

        @Schema(description = "카테고리명", example = "브레이크")
        private String categoryName;

        @Schema(description = "판매량 (수량)", example = "50")
        private Long salesCount;
    }
}

