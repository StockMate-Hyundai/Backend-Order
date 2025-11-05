package com.stockmate.order.api.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "월별 TOP 매출량/순이익 리포트 요청 DTO")
public class TopSalesRequestDTO {

    @NotNull(message = "년도는 필수입니다.")
    @Min(value = 2000, message = "년도는 2000년 이상이어야 합니다.")
    @Max(value = 2100, message = "년도는 2100년 이하여야 합니다.")
    @Schema(description = "년도", example = "2024")
    private Integer year;

    @NotNull(message = "월은 필수입니다.")
    @Min(value = 1, message = "월은 1 이상이어야 합니다.")
    @Max(value = 12, message = "월은 12 이하여야 합니다.")
    @Schema(description = "월", example = "11")
    private Integer month;
}

