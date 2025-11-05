package com.stockmate.order.api.report.controller;

import com.stockmate.order.api.report.dto.DailyCategorySalesRequestDTO;
import com.stockmate.order.api.report.dto.DailyCategorySalesResponseDTO;
import com.stockmate.order.api.report.dto.DailyReportRequestDTO;
import com.stockmate.order.api.report.dto.DailyReportResponseDTO;
import com.stockmate.order.api.report.dto.MonthlyReportRequestDTO;
import com.stockmate.order.api.report.dto.MonthlyReportResponseDTO;
import com.stockmate.order.api.report.dto.TopSalesRequestDTO;
import com.stockmate.order.api.report.dto.TopSalesResponseDTO;
import com.stockmate.order.api.report.dto.WarehouseReportRequestDTO;
import com.stockmate.order.api.report.dto.WarehouseReportResponseDTO;
import com.stockmate.order.api.report.dto.WeeklyReportRequestDTO;
import com.stockmate.order.api.report.dto.WeeklyReportResponseDTO;
import com.stockmate.order.api.report.service.ReportService;
import com.stockmate.order.common.response.ApiResponse;
import com.stockmate.order.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/order/report")
@RequiredArgsConstructor
@Tag(name = "Report", description = "리포트 관련 API 입니다.")
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "월별 리포트 조회", description = "지정된 년월의 주문/출고/매출/원가/순수익 집계 데이터를 조회합니다.")
    @GetMapping("/monthly")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MonthlyReportResponseDTO>> getMonthlyReport(@Valid @ModelAttribute MonthlyReportRequestDTO requestDTO) {

        log.info("월별 리포트 조회 요청 - 년월: {}-{}", requestDTO.getYear(), requestDTO.getMonth());
        MonthlyReportResponseDTO response = reportService.getMonthlyReport(requestDTO);

        log.info("월별 리포트 조회 완료 - 주문: {}건, 출고: {}건, 매출: {}원, 순수익: {}원", 
                response.getTotalOrderCount(), 
                response.getTotalShippedCount(), 
                response.getTotalRevenue(), 
                response.getNetProfit());

        return ApiResponse.success(SuccessStatus.GET_MONTHLY_REPORT_SUCCESS, response);
    }

    @Operation(summary = "주차별 리포트 조회", description = "지정된 년월의 이전 월 마지막 2주차 + 해당 월 전체 4주차 = 총 7주차의 주문/출고 | 판매수익/순수익 데이터를 조회합니다.")
    @GetMapping("/weekly")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<WeeklyReportResponseDTO>> getWeeklyReport(@Valid @ModelAttribute WeeklyReportRequestDTO requestDTO) {

        log.info("주차별 리포트 조회 요청 - 년월: {}-{}", requestDTO.getYear(), requestDTO.getMonth());
        WeeklyReportResponseDTO response = reportService.getWeeklyReport(requestDTO);

        log.info("주차별 리포트 조회 완료 - 총 {}주차 데이터", response.getWeeks().size());
        return ApiResponse.success(SuccessStatus.GET_WEEKLY_REPORT_SUCCESS, response);
    }

    @Operation(summary = "일자별 리포트 조회", description = "지정된 년월의 모든 일자별 주문량과 출고량을 조회합니다.")
    @GetMapping("/daily")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DailyReportResponseDTO>> getDailyReport(@Valid @ModelAttribute DailyReportRequestDTO requestDTO) {

        log.info("일자별 리포트 조회 요청 - 년월: {}-{}", requestDTO.getYear(), requestDTO.getMonth());
        DailyReportResponseDTO response = reportService.getDailyReport(requestDTO);

        log.info("일자별 리포트 조회 완료 - 총 {}일 데이터", response.getDays().size());
        return ApiResponse.success(SuccessStatus.GET_DAILY_REPORT_SUCCESS, response);
    }

    @Operation(summary = "일자별 카테고리별 판매량 리포트 조회", 
               description = "지정된 년월의 모든 일자별 카테고리별 판매량을 조회합니다.")
    @GetMapping("/daily/category-sales")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DailyCategorySalesResponseDTO>> getDailyCategorySales(
            @Valid @ModelAttribute DailyCategorySalesRequestDTO requestDTO) {

        log.info("일자별 카테고리별 판매량 리포트 조회 요청 - 년월: {}-{}", requestDTO.getYear(), requestDTO.getMonth());

        DailyCategorySalesResponseDTO response = reportService.getDailyCategorySales(requestDTO);

        log.info("일자별 카테고리별 판매량 리포트 조회 완료 - 총 {}일 데이터", response.getDays().size());
        return ApiResponse.success(SuccessStatus.GET_DAILY_CATEGORY_SALES_SUCCESS, response);
    }

    @Operation(summary = "월별 TOP 매출량/순이익 리포트 조회", 
               description = "지정된 년월의 TOP 10 매출량 부품과 TOP 10 순이익 부품을 조회합니다.")
    @GetMapping("/top-sales")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TopSalesResponseDTO>> getTopSales(
            @Valid @ModelAttribute TopSalesRequestDTO requestDTO) {

        log.info("월별 TOP 매출량/순이익 리포트 조회 요청 - 년월: {}-{}", requestDTO.getYear(), requestDTO.getMonth());

        TopSalesResponseDTO response = reportService.getTopSales(requestDTO);

        log.info("월별 TOP 매출량/순이익 리포트 조회 완료 - TOP 매출량: {}개, TOP 순이익: {}개", 
                response.getTopRevenue().size(), response.getTopProfit().size());
        return ApiResponse.success(SuccessStatus.GET_TOP_SALES_SUCCESS, response);
    }

    @Operation(summary = "월별 창고별 리포트 조회", 
               description = "지정된 년월의 창고별(A, B, C, D, E) 총 주문수, 총 출고수, 주문수 비율(%)을 조회합니다.")
    @GetMapping("/warehouse")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<WarehouseReportResponseDTO>> getWarehouseReport(
            @Valid @ModelAttribute WarehouseReportRequestDTO requestDTO) {

        log.info("월별 창고별 리포트 조회 요청 - 년월: {}-{}", requestDTO.getYear(), requestDTO.getMonth());

        WarehouseReportResponseDTO response = reportService.getWarehouseReport(requestDTO);

        log.info("월별 창고별 리포트 조회 완료 - 창고 수: {}개", response.getWarehouses().size());
        return ApiResponse.success(SuccessStatus.GET_WAREHOUSE_REPORT_SUCCESS, response);
    }
}

