package com.stockmate.order.api.report.service;

import com.stockmate.order.api.order.repository.OrderRepository;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final OrderRepository orderRepository;

    // 월별 리포트 조회
    public MonthlyReportResponseDTO getMonthlyReport(MonthlyReportRequestDTO requestDTO) {
        int year = requestDTO.getYear();
        int month = requestDTO.getMonth();

        log.info("월별 리포트 조회 시작 - 년월: {}-{}", year, month);

        // 기본 집계 데이터 조회
        Long totalOrderCount = orderRepository.countMonthlyOrders(year, month);
        Long totalShippedCount = orderRepository.countMonthlyShippedOrders(year, month);
        Long totalOrderItemCount = orderRepository.sumMonthlyOrderItemCount(year, month);
        Long totalShippedItemCount = orderRepository.sumMonthlyShippedItemCount(year, month);
        Long totalRevenue = orderRepository.calculateMonthlyRevenue(year, month);
        Long totalCost = orderRepository.calculateMonthlyCost(year, month);

        // 순수익 계산
        Long netProfit = totalRevenue - totalCost;

        log.info("월별 리포트 조회 완료 - 주문: {}건, 출고: {}건, 매출: {}원, 원가: {}원, 순수익: {}원", 
                totalOrderCount, totalShippedCount, totalRevenue, totalCost, netProfit);

        return MonthlyReportResponseDTO.builder()
                .year(year)
                .month(month)
                .totalOrderCount(totalOrderCount)
                .totalShippedCount(totalShippedCount)
                .totalOrderItemCount(totalOrderItemCount)
                .totalShippedItemCount(totalShippedItemCount)
                .totalRevenue(totalRevenue)
                .totalCost(totalCost)
                .netProfit(netProfit)
                .build();
    }

    /**
     * 주차별 리포트 조회 (7주차)
     * - 이전 월의 마지막 2주차 (3주차, 4주차)
     * - 현재 월의 전체 4주차
     */
    public WeeklyReportResponseDTO getWeeklyReport(WeeklyReportRequestDTO requestDTO) {
        int year = requestDTO.getYear();
        int month = requestDTO.getMonth();

        log.info("주차별 리포트 조회 시작 - 년월: {}-{}", year, month);

        List<WeeklyReportResponseDTO.WeekData> weeks = new ArrayList<>();

        // 이전 월의 마지막 2주차 (3주차, 4주차)
        YearMonth previousMonth = YearMonth.of(year, month).minusMonths(1);
        int prevYear = previousMonth.getYear();
        int prevMonth = previousMonth.getMonthValue();

        weeks.add(getWeekData(prevYear, prevMonth, 3));
        weeks.add(getWeekData(prevYear, prevMonth, 4));

        // 현재 월의 전체 4주차
        weeks.add(getWeekData(year, month, 1));
        weeks.add(getWeekData(year, month, 2));
        weeks.add(getWeekData(year, month, 3));
        weeks.add(getWeekData(year, month, 4));

        log.info("주차별 리포트 조회 완료 - 총 {}주차 데이터", weeks.size());

        return WeeklyReportResponseDTO.builder()
                .year(year)
                .month(month)
                .weeks(weeks)
                .build();
    }

    // 특정 주차의 데이터 조회
    private WeeklyReportResponseDTO.WeekData getWeekData(int year, int month, int week) {
        // 주차별 시작일/종료일 계산
        LocalDate startDate;
        LocalDate endDate;

        switch (week) {
            case 1:
                startDate = LocalDate.of(year, month, 1);
                endDate = LocalDate.of(year, month, 7);
                break;
            case 2:
                startDate = LocalDate.of(year, month, 8);
                endDate = LocalDate.of(year, month, 14);
                break;
            case 3:
                startDate = LocalDate.of(year, month, 15);
                endDate = LocalDate.of(year, month, 21);
                break;
            case 4:
                startDate = LocalDate.of(year, month, 22);
                YearMonth yearMonth = YearMonth.of(year, month);
                endDate = LocalDate.of(year, month, yearMonth.lengthOfMonth());
                break;
            default:
                throw new IllegalArgumentException("주차는 1~4만 가능합니다: " + week);
        }

        // LocalDateTime으로 변환 (시작: 00:00:00, 종료: 다음날 00:00:00)
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        Long orderCount = orderRepository.countOrdersByDateRange(startDateTime, endDateTime);
        Long shippedCount = orderRepository.countShippedOrdersByDateRange(startDateTime, endDateTime);
        Long revenue = orderRepository.calculateRevenueByDateRange(startDateTime, endDateTime);
        Long cost = orderRepository.calculateCostByDateRange(startDateTime, endDateTime);
        Long netProfit = revenue - cost;

        log.debug("주차 데이터 조회 완료 - {}년 {}월 {}주차: 주문 {}건, 출고 {}건, 매출 {}원, 원가 {}원, 순수익 {}원", 
                year, month, week, orderCount, shippedCount, revenue, cost, netProfit);

        return WeeklyReportResponseDTO.WeekData.builder()
                .year(year)
                .month(month)
                .week(week)
                .weekLabel(month + "월 " + week + "주차")
                .startDate(startDate.toString())
                .endDate(endDate.toString())
                .totalOrderCount(orderCount)
                .totalShippedCount(shippedCount)
                .totalRevenue(revenue)
                .totalCost(cost)
                .netProfit(netProfit)
                .build();
    }

    // 일자별 리포트 조회
    public DailyReportResponseDTO getDailyReport(DailyReportRequestDTO requestDTO) {
        int year = requestDTO.getYear();
        int month = requestDTO.getMonth();

        log.info("일자별 리포트 조회 시작 - 년월: {}-{}", year, month);

        // 1. DB에서 일자별 집계 데이터 조회
        List<Object[]> orderCounts = orderRepository.countDailyOrders(year, month);
        List<Object[]> shippedCounts = orderRepository.countDailyShippedOrders(year, month);

        // 2. Map으로 변환 (빠른 조회를 위해)
        Map<Integer, Long> orderCountMap = new HashMap<>();
        for (Object[] row : orderCounts) {
            int day = (Integer) row[2]; // DAY
            long count = (Long) row[3]; // COUNT
            orderCountMap.put(day, count);
        }

        Map<Integer, Long> shippedCountMap = new HashMap<>();
        for (Object[] row : shippedCounts) {
            int day = (Integer) row[2]; // DAY
            long count = (Long) row[3]; // COUNT
            shippedCountMap.put(day, count);
        }

        // 3. 해당 월의 모든 일자 생성 (데이터가 없는 일자도 0으로 표시)
        YearMonth yearMonth = YearMonth.of(year, month);
        int daysInMonth = yearMonth.lengthOfMonth();
        List<DailyReportResponseDTO.DayData> dayDataList = new ArrayList<>();

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = LocalDate.of(year, month, day);
            DayOfWeek dayOfWeek = date.getDayOfWeek();

            // 요일 번호 (1=월요일, 7=일요일)
            int dayOfWeekValue = dayOfWeek.getValue();

            // 요일명 (한국어)
            String dayOfWeekName = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN);

            Long orderCount = orderCountMap.getOrDefault(day, 0L);
            Long shippedCount = shippedCountMap.getOrDefault(day, 0L);

            dayDataList.add(DailyReportResponseDTO.DayData.builder()
                    .year(year)
                    .month(month)
                    .day(day)
                    .date(date.toString())
                    .dayOfWeek(dayOfWeekValue)
                    .dayOfWeekName(dayOfWeekName)
                    .totalOrderCount(orderCount)
                    .totalShippedCount(shippedCount)
                    .build());
        }

        log.info("일자별 리포트 조회 완료 - 총 {}일 데이터", dayDataList.size());

        return DailyReportResponseDTO.builder()
                .year(year)
                .month(month)
                .days(dayDataList)
                .build();
    }

    // 일자별 카테고리별 판매량 리포트 조회
    public DailyCategorySalesResponseDTO getDailyCategorySales(DailyCategorySalesRequestDTO requestDTO) {
        int year = requestDTO.getYear();
        int month = requestDTO.getMonth();

        log.info("일자별 카테고리별 판매량 리포트 조회 시작 - 년월: {}-{}", year, month);

        // 1. DB에서 일자별 카테고리별 판매량 조회
        List<Object[]> dailyCategorySales = orderRepository.getDailyCategorySales(year, month);

        // 2. Map으로 변환: day -> Map<categoryName, salesCount>
        Map<Integer, Map<String, Long>> dayCategoryMap = new HashMap<>();
        for (Object[] row : dailyCategorySales) {
            int day = (Integer) row[2]; // DAY
            String categoryName = (String) row[3]; // categoryName
            Long salesCount = ((Number) row[4]).longValue(); // SUM(amount)

            dayCategoryMap.putIfAbsent(day, new HashMap<>());
            dayCategoryMap.get(day).put(categoryName, salesCount);
        }

        // 3. 해당 월의 모든 일자 생성 (데이터가 없는 일자도 포함)
        YearMonth yearMonth = YearMonth.of(year, month);
        int daysInMonth = yearMonth.lengthOfMonth();
        List<DailyCategorySalesResponseDTO.DayCategoryData> dayDataList = new ArrayList<>();

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = LocalDate.of(year, month, day);

            // 해당 일자의 카테고리별 판매량 조회
            Map<String, Long> categoryMap = dayCategoryMap.getOrDefault(day, new HashMap<>());
            List<DailyCategorySalesResponseDTO.CategorySales> categories = new ArrayList<>();

            for (Map.Entry<String, Long> entry : categoryMap.entrySet()) {
                categories.add(DailyCategorySalesResponseDTO.CategorySales.builder()
                        .categoryName(entry.getKey())
                        .salesCount(entry.getValue())
                        .build());
            }

            // 카테고리명으로 정렬 (일관된 순서)
            categories.sort((a, b) -> a.getCategoryName().compareTo(b.getCategoryName()));

            dayDataList.add(DailyCategorySalesResponseDTO.DayCategoryData.builder()
                    .year(year)
                    .month(month)
                    .day(day)
                    .date(date.toString())
                    .categories(categories)
                    .build());
        }

        log.info("일자별 카테고리별 판매량 리포트 조회 완료 - 총 {}일 데이터", dayDataList.size());

        return DailyCategorySalesResponseDTO.builder()
                .year(year)
                .month(month)
                .days(dayDataList)
                .build();
    }

    // 월별 TOP 매출량/순이익 리포트 조회
    public TopSalesResponseDTO getTopSales(TopSalesRequestDTO requestDTO) {
        int year = requestDTO.getYear();
        int month = requestDTO.getMonth();

        log.info("월별 TOP 매출량/순이익 리포트 조회 시작 - 년월: {}-{}", year, month);

        // 1. TOP 10 매출량 부품 조회
        List<Object[]> topRevenueData = orderRepository.getTopRevenueParts(year, month);
        List<TopSalesResponseDTO.PartSalesData> topRevenueList = new ArrayList<>();

        for (int i = 0; i < Math.min(10, topRevenueData.size()); i++) {
            Object[] row = topRevenueData.get(i);
            int rank = i + 1; // 1위부터 시작
            topRevenueList.add(convertToPartSalesData(row, rank));
        }

        // 2. TOP 10 순이익 부품 조회
        List<Object[]> topProfitData = orderRepository.getTopProfitParts(year, month);
        List<TopSalesResponseDTO.PartSalesData> topProfitList = new ArrayList<>();

        for (int i = 0; i < Math.min(10, topProfitData.size()); i++) {
            Object[] row = topProfitData.get(i);
            int rank = i + 1; // 1위부터 시작
            topProfitList.add(convertToPartSalesData(row, rank));
        }

        log.info("월별 TOP 매출량/순이익 리포트 조회 완료 - TOP 매출량: {}개, TOP 순이익: {}개", 
                topRevenueList.size(), topProfitList.size());

        return TopSalesResponseDTO.builder()
                .year(year)
                .month(month)
                .topRevenue(topRevenueList)
                .topProfit(topProfitList)
                .build();
    }

    // 쿼리 결과를 PartSalesData로 변환
    private TopSalesResponseDTO.PartSalesData convertToPartSalesData(Object[] row, int rank) {
        Long partId = ((Number) row[0]).longValue(); // partId
        String partName = (String) row[1]; // name
        String categoryName = (String) row[2]; // categoryName
        Long quantity = ((Number) row[3]).longValue(); // SUM(amount)
        Long unitPrice = ((Number) row[4]).longValue(); // price
        Long totalRevenue = ((Number) row[5]).longValue(); // SUM(price * amount)
        Long totalCost = ((Number) row[6]).longValue(); // SUM(cost * amount)
        Long netProfit = ((Number) row[7]).longValue(); // SUM((price - cost) * amount)

        return TopSalesResponseDTO.PartSalesData.builder()
                .rank(rank)
                .partId(partId)
                .partName(partName)
                .categoryName(categoryName)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .totalRevenue(totalRevenue)
                .totalCost(totalCost)
                .netProfit(netProfit)
                .build();
    }

    // 월별 창고별 리포트 조회
    public WarehouseReportResponseDTO getWarehouseReport(WarehouseReportRequestDTO requestDTO) {
        int year = requestDTO.getYear();
        int month = requestDTO.getMonth();

        log.info("월별 창고별 리포트 조회 시작 - 년월: {}-{}", year, month);

        // 1. 창고별 주문 건수 조회
        List<Object[]> orderCounts = orderRepository.countOrdersByWarehouse(year, month);
        List<Object[]> shippedCounts = orderRepository.countShippedOrdersByWarehouse(year, month);

        // 2. Map으로 변환
        Map<String, Long> orderCountMap = new HashMap<>();
        for (Object[] row : orderCounts) {
            String warehouse = (String) row[0]; // SUBSTRING(oi.location, 1, 1)
            Long count = ((Number) row[1]).longValue(); // COUNT(DISTINCT o.orderId)
            orderCountMap.put(warehouse, count);
        }

        Map<String, Long> shippedCountMap = new HashMap<>();
        for (Object[] row : shippedCounts) {
            String warehouse = (String) row[0]; // SUBSTRING(oi.location, 1, 1)
            Long count = ((Number) row[1]).longValue(); // COUNT(DISTINCT o.orderId)
            shippedCountMap.put(warehouse, count);
        }

        // 3. 전체 주문 수 계산 (비율 계산용)
        long totalOrderCount = orderCountMap.values().stream().mapToLong(Long::longValue).sum();

        // 4. 창고별 데이터 생성 (A, B, C, D, E 순서)
        List<WarehouseReportResponseDTO.WarehouseData> warehouseDataList = new ArrayList<>();
        String[] warehouses = {"A", "B", "C", "D", "E"};

        for (String warehouse : warehouses) {
            Long orderCount = orderCountMap.getOrDefault(warehouse, 0L);
            Long shippedCount = shippedCountMap.getOrDefault(warehouse, 0L);

            // 비율 계산 (전체 주문 수 대비)
            double orderPercentage = totalOrderCount > 0 
                    ? (orderCount.doubleValue() / totalOrderCount) * 100.0 
                    : 0.0;

            warehouseDataList.add(WarehouseReportResponseDTO.WarehouseData.builder()
                    .warehouse(warehouse)
                    .totalOrderCount(orderCount)
                    .totalShippedCount(shippedCount)
                    .orderPercentage(orderPercentage)
                    .build());
        }

        log.info("월별 창고별 리포트 조회 완료 - 총 주문 수: {}, 창고별 데이터: {}개", 
                totalOrderCount, warehouseDataList.size());

        return WarehouseReportResponseDTO.builder()
                .year(year)
                .month(month)
                .warehouses(warehouseDataList)
                .build();
    }
}

