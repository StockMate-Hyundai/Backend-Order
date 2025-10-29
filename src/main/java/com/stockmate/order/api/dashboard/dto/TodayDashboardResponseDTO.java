package com.stockmate.order.api.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodayDashboardResponseDTO {
    private TodaySummary summary;           // 금일 전체 요약
    private List<HourlyStats> hourlyStats;  // 시간대별 통계

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TodaySummary {
        private long totalOrders;           // 금일 주문 수
        private long shippingProcessed;     // 금일 배송 처리된 수
        private long shippingInProgress;    // 금일 배송 중인 수
        private long totalRevenue;          // 금일 매출 금액
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HourlyStats {
        private int hour;                   // 시간 (0-23)
        private long orderCount;            // 해당 시간대 주문 수
        private long shippingProcessedCount; // 해당 시간대 배송 처리 수
        private long shippingInProgressCount; // 해당 시간대 배송 중인 수
        private long revenue;               // 해당 시간대 매출
    }
}

