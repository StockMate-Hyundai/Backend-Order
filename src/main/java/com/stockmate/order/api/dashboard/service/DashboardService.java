package com.stockmate.order.api.dashboard.service;

import com.stockmate.order.api.dashboard.dto.HourlyInOutResponseDTO;
import com.stockmate.order.api.dashboard.dto.TodayDashboardResponseDTO;
import com.stockmate.order.api.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public TodayDashboardResponseDTO getTodayDashboard() {
        log.info("금일 대시보드 조회 시작");

        // 금일 시작/종료 시간 계산
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);

        log.info("조회 기간 - 시작: {}, 종료: {}", startOfDay, endOfDay);

        // 전체 요약 데이터 조회
        long totalOrders = orderRepository.countTodayOrders(startOfDay, endOfDay);
        long shippingProcessed = orderRepository.countTodayShippingProcessed(startOfDay, endOfDay);
        long shippingInProgress = orderRepository.countTodayShippingInProgress(startOfDay, endOfDay);
        long totalRevenue = orderRepository.calculateTodayRevenue(startOfDay, endOfDay);

        log.info("금일 요약 - 주문: {}, 배송처리: {}, 배송중: {}, 매출: {}", 
                totalOrders, shippingProcessed, shippingInProgress, totalRevenue);

        TodayDashboardResponseDTO.TodaySummary summary = TodayDashboardResponseDTO.TodaySummary.builder()
                .totalOrders(totalOrders)
                .shippingProcessed(shippingProcessed)
                .shippingInProgress(shippingInProgress)
                .totalRevenue(totalRevenue)
                .build();

        // 시간대별 통계 조회
        List<Object[]> ordersByHour = orderRepository.countOrdersByHour(startOfDay, endOfDay);
        List<Object[]> shippingProcessedByHour = orderRepository.countShippingProcessedByHour(startOfDay, endOfDay);
        List<Object[]> shippingInProgressByHour = orderRepository.countShippingInProgressByHour(startOfDay, endOfDay);
        List<Object[]> revenueByHour = orderRepository.calculateRevenueByHour(startOfDay, endOfDay);

        // 시간대별 데이터를 Map으로 변환
        Map<Integer, Long> orderCountMap = convertToMap(ordersByHour);
        Map<Integer, Long> shippingProcessedMap = convertToMap(shippingProcessedByHour);
        Map<Integer, Long> shippingInProgressMap = convertToMap(shippingInProgressByHour);
        Map<Integer, Long> revenueMap = convertToMap(revenueByHour);

        // 0~23시까지 모든 시간대 데이터 생성 (데이터 없는 시간은 0으로)
        List<TodayDashboardResponseDTO.HourlyStats> hourlyStats = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            hourlyStats.add(TodayDashboardResponseDTO.HourlyStats.builder()
                    .hour(hour)
                    .orderCount(orderCountMap.getOrDefault(hour, 0L))
                    .shippingProcessedCount(shippingProcessedMap.getOrDefault(hour, 0L))
                    .shippingInProgressCount(shippingInProgressMap.getOrDefault(hour, 0L))
                    .revenue(revenueMap.getOrDefault(hour, 0L))
                    .build());
        }

        log.info("금일 대시보드 조회 완료 - 시간대별 데이터 수: {}", hourlyStats.size());

        return TodayDashboardResponseDTO.builder()
                .summary(summary)
                .hourlyStats(hourlyStats)
                .build();
    }

    // 금일 시간대별 입출고 추이
    @Transactional(readOnly = true)
    public HourlyInOutResponseDTO getTodayInboundOutbound() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);

        // 주문 생성(입고) 시간대별
        List<Object[]> ordersByHour = orderRepository.countOrdersByHour(startOfDay, endOfDay);
        Map<Integer, Long> orderCountMap = convertToMap(ordersByHour);

        // 배송 처리(출고) 시간대별
        List<Object[]> shippingProcessedByHour = orderRepository.countShippingProcessedByHour(startOfDay, endOfDay);
        Map<Integer, Long> shippingProcessedMap = convertToMap(shippingProcessedByHour);

        List<com.stockmate.order.api.dashboard.dto.HourlyInOutResponseDTO.HourStat> list = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            list.add(com.stockmate.order.api.dashboard.dto.HourlyInOutResponseDTO.HourStat.builder()
                    .hour(hour)
                    .inboundOrders(orderCountMap.getOrDefault(hour, 0L))
                    .outboundShipped(shippingProcessedMap.getOrDefault(hour, 0L))
                    .build());
        }

        return HourlyInOutResponseDTO.builder()
                .hours(list)
                .build();
    }

    /**
     * Object[] 배열을 Map으로 변환
     * Object[0]: hour (Integer)
     * Object[1]: count or revenue (Long)
     */
    private Map<Integer, Long> convertToMap(List<Object[]> data) {
        Map<Integer, Long> map = new HashMap<>();
        for (Object[] row : data) {
            Integer hour = (Integer) row[0];
            Long value = ((Number) row[1]).longValue();
            map.put(hour, value);
        }
        return map;
    }
}

