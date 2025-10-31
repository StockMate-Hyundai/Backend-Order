package com.stockmate.order.api.dashboard.controller;

import com.stockmate.order.api.dashboard.dto.CategorySalesResponseDTO;
import com.stockmate.order.api.dashboard.dto.HourlyInOutResponseDTO;
import com.stockmate.order.api.dashboard.dto.RecentOrdersResponseDTO;
import com.stockmate.order.api.dashboard.dto.TodayDashboardResponseDTO;
import com.stockmate.order.api.dashboard.dto.TopPartsResponseDTO;
import com.stockmate.order.api.dashboard.service.DashboardService;
import com.stockmate.order.common.response.ApiResponse;
import com.stockmate.order.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/order/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dashboard", description = "대시보드 관련 API 입니다.")
public class DashboardController {

	private final DashboardService dashboardService;

	@Operation(
			summary = "대시보드 조회",
			description = "지정된 날짜의 주문 수, 배송 처리 수, 배송 중인 수, 매출 금액 및 시간대별 추이를 조회합니다. 날짜 미지정 시 오늘 날짜로 조회합니다."
	)
	@GetMapping("/today")
	@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
	public ResponseEntity<ApiResponse<TodayDashboardResponseDTO>> getTodayDashboard(@RequestParam(required = false) String date) {
		
		log.info("대시보드 조회 요청 - 날짜: {}", date != null ? date : "오늘");

		TodayDashboardResponseDTO response = dashboardService.getDashboard(date);

		log.info("대시보드 조회 완료 - 총 주문: {}, 매출: {}",
				response.getSummary().getTotalOrders(),
				response.getSummary().getTotalRevenue());

		return ApiResponse.success(SuccessStatus.GET_TODAY_DASHBOARD_SUCCESS, response);
	}

	@Operation(
			summary = "시간대별 입출고 추이",
			description = "지정된 날짜의 0~23시 시간대별로 입고(주문) 수, 출고(배송 처리) 수를 조회합니다. 날짜 미지정 시 오늘 날짜로 조회합니다."
	)
	@GetMapping("/today/inout")
	@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
	public ResponseEntity<ApiResponse<HourlyInOutResponseDTO>> getTodayInboundOutbound(@RequestParam(required = false) String date) {

        var response = dashboardService.getInboundOutbound(date);
		return ApiResponse.success(SuccessStatus.GET_TODAY_INOUT_DASHBOARD_SUCCESS, response);
	}

	@Operation(
			summary = "카테고리별 판매량 조회",
			description = "지정된 날짜의 카테고리별 판매 수량을 조회합니다. 날짜 미지정 시 오늘 날짜로 조회합니다."
	)
	@GetMapping("/category-sales")
	@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
	public ResponseEntity<ApiResponse<CategorySalesResponseDTO>> getCategorySales(@RequestParam(required = false) String date) {

		log.info("카테고리별 판매량 조회 요청 - 날짜: {}", date != null ? date : "오늘");
		CategorySalesResponseDTO response = dashboardService.getCategorySales(date);

		log.info("카테고리별 판매량 조회 완료 - 카테고리 수: {}", response.getCategories().size());
		return ApiResponse.success(SuccessStatus.GET_CATEGORY_SALES_SUCCESS, response);
	}

	@Operation(
			summary = "최근 주문 이력 조회",
			description = "지정된 날짜의 최근 10개 주문 이력을 조회합니다. 날짜 미지정 시 오늘 날짜로 조회합니다."
	)
	@GetMapping("/recent-orders")
	@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
	public ResponseEntity<ApiResponse<RecentOrdersResponseDTO>> getRecentOrders(@RequestParam(required = false) String date) {

		log.info("최근 주문 이력 조회 요청 - 날짜: {}", date != null ? date : "오늘");
		RecentOrdersResponseDTO response = dashboardService.getRecentOrders(date);

		log.info("최근 주문 이력 조회 완료 - 주문 수: {}", response.getOrders().size());
		return ApiResponse.success(SuccessStatus.GET_RECENT_ORDERS_SUCCESS, response);
	}

	@Operation(
			summary = "TOP 판매 부품 조회",
			description = "지정된 날짜의 상위 10개 판매 부품(부품명, 카테고리명, 판매수량)을 조회합니다. 날짜 미지정 시 오늘 날짜로 조회합니다."
	)
	@GetMapping("/top-parts")
	@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
	public ResponseEntity<ApiResponse<TopPartsResponseDTO>> getTopParts(@RequestParam(required = false) String date) {

		log.info("TOP 판매 부품 조회 요청 - 날짜: {}", date != null ? date : "오늘");
		var response = dashboardService.getTopParts(date);
		return ApiResponse.success(SuccessStatus.GET_TOP_PARTS_SUCCESS, response);
	}
}

