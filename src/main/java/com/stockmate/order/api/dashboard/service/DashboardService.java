package com.stockmate.order.api.dashboard.service;

import com.stockmate.order.api.dashboard.dto.RecentOrdersResponseDTO;
import com.stockmate.order.api.dashboard.dto.TodayDashboardResponseDTO;
import com.stockmate.order.api.order.dto.UserBatchResponseDTO;
import com.stockmate.order.api.order.repository.OrderRepository;
import com.stockmate.order.api.order.service.UserService;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final OrderRepository orderRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public TodayDashboardResponseDTO getDashboard(String date) {
        // 날짜 파싱 (미지정 시 오늘 날짜)
        LocalDate targetDate;
        if (date == null || date.isEmpty()) {
            targetDate = LocalDate.now();
            log.info("대시보드 조회 시작 - 날짜 미지정, 오늘 날짜 사용: {}", targetDate);
        } else {
            try {
                targetDate = LocalDate.parse(date);
                log.info("대시보드 조회 시작 - 지정된 날짜: {}", targetDate);
            } catch (Exception e) {
                log.error("날짜 파싱 실패 - 입력값: {}, 오늘 날짜로 대체", date);
                targetDate = LocalDate.now();
            }
        }

        // 해당 날짜의 시작/종료 시간 계산
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.atTime(23, 59, 59);

        log.info("조회 기간 - 시작: {}, 종료: {}", startOfDay, endOfDay);

        // 전체 요약 데이터 조회
        long totalOrders = orderRepository.countTodayOrders(startOfDay, endOfDay);
        long shippingProcessed = orderRepository.countTodayShippingProcessed(startOfDay, endOfDay);
        long shippingInProgress = orderRepository.countTodayShippingInProgress(startOfDay, endOfDay);
        long totalRevenue = orderRepository.calculateTodayRevenue(startOfDay, endOfDay);

        log.info("요약 데이터 - 주문: {}, 배송처리: {}, 배송중: {}, 매출: {}", 
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

        log.info("대시보드 조회 완료 - 날짜: {}, 시간대별 데이터 수: {}", targetDate, hourlyStats.size());

        return TodayDashboardResponseDTO.builder()
                .summary(summary)
                .hourlyStats(hourlyStats)
                .build();
    }

    // 시간대별 입출고 추이
    @Transactional(readOnly = true)
    public com.stockmate.order.api.dashboard.dto.HourlyInOutResponseDTO getInboundOutbound(String date) {
        // 날짜 파싱 (미지정 시 오늘 날짜)
        LocalDate targetDate;
        if (date == null || date.isEmpty()) {
            targetDate = LocalDate.now();
            log.info("입출고 추이 조회 시작 - 날짜 미지정, 오늘 날짜 사용: {}", targetDate);
        } else {
            try {
                targetDate = LocalDate.parse(date);
                log.info("입출고 추이 조회 시작 - 지정된 날짜: {}", targetDate);
            } catch (Exception e) {
                log.error("날짜 파싱 실패 - 입력값: {}, 오늘 날짜로 대체", date);
                targetDate = LocalDate.now();
            }
        }

        // 해당 날짜의 시작/종료 시간 계산
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.atTime(23, 59, 59);

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

        return com.stockmate.order.api.dashboard.dto.HourlyInOutResponseDTO.builder()
                .hours(list)
                .build();
    }

    // 카테고리별 판매량 조회
    @Transactional(readOnly = true)
    public com.stockmate.order.api.dashboard.dto.CategorySalesResponseDTO getCategorySales(String date) {
        // 날짜 파싱 (미지정 시 오늘 날짜)
        LocalDate targetDate;
        if (date == null || date.isEmpty()) {
            targetDate = LocalDate.now();
            log.info("카테고리별 판매량 조회 시작 - 날짜 미지정, 오늘 날짜 사용: {}", targetDate);
        } else {
            try {
                targetDate = LocalDate.parse(date);
                log.info("카테고리별 판매량 조회 시작 - 지정된 날짜: {}", targetDate);
            } catch (Exception e) {
                log.error("날짜 파싱 실패 - 입력값: {}, 오늘 날짜로 대체", date);
                targetDate = LocalDate.now();
            }
        }

        // 해당 날짜의 시작/종료 시간 계산
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.atTime(23, 59, 59);

        log.info("카테고리별 판매량 조회 기간 - 시작: {}, 종료: {}", startOfDay, endOfDay);

        // 카테고리별 판매량 조회
        List<Object[]> categorySalesData = orderRepository.getCategorySalesByDate(startOfDay, endOfDay);

        // 카테고리별 판매량 DTO로 변환
        List<com.stockmate.order.api.dashboard.dto.CategorySalesResponseDTO.CategorySale> categories = new ArrayList<>();
        for (Object[] row : categorySalesData) {
            String categoryName = (String) row[0];
            Long totalQuantity = ((Number) row[1]).longValue();
            
            categories.add(com.stockmate.order.api.dashboard.dto.CategorySalesResponseDTO.CategorySale.builder()
                    .categoryName(categoryName)
                    .totalQuantity(totalQuantity)
                    .build());
        }

        log.info("카테고리별 판매량 조회 완료 - 날짜: {}, 카테고리 수: {}", targetDate, categories.size());

        return com.stockmate.order.api.dashboard.dto.CategorySalesResponseDTO.builder()
                .categories(categories)
                .build();
    }

    // TOP 판매 부품 조회 (최대 10개)
    @Transactional(readOnly = true)
    public com.stockmate.order.api.dashboard.dto.TopPartsResponseDTO getTopParts(String date) {
        // 날짜 파싱 (미지정 시 오늘 날짜)
        LocalDate targetDate;
        if (date == null || date.isEmpty()) {
            targetDate = LocalDate.now();
            log.info("TOP 판매 부품 조회 시작 - 날짜 미지정, 오늘 날짜 사용: {}", targetDate);
        } else {
            try {
                targetDate = LocalDate.parse(date);
                log.info("TOP 판매 부품 조회 시작 - 지정된 날짜: {}", targetDate);
            } catch (Exception e) {
                log.error("날짜 파싱 실패 - 입력값: {}, 오늘 날짜로 대체", date);
                targetDate = LocalDate.now();
            }
        }

        // 해당 날짜의 시작/종료 시간 계산
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.atTime(23, 59, 59);

        log.info("TOP 판매 부품 조회 기간 - 시작: {}, 종료: {}", startOfDay, endOfDay);

        // 집계 쿼리 호출
        List<Object[]> rows = orderRepository.getTopPartsByDate(startOfDay, endOfDay);

        // 상위 10개로 제한 및 DTO 변환
        List<com.stockmate.order.api.dashboard.dto.TopPartsResponseDTO.TopPart> parts = rows.stream()
                .limit(10)
                .map(row -> com.stockmate.order.api.dashboard.dto.TopPartsResponseDTO.TopPart.builder()
                        .name((String) row[0])
                        .categoryName((String) row[1])
                        .salesCount(((Number) row[2]).longValue())
                        .build())
                .collect(java.util.stream.Collectors.toList());

        log.info("TOP 판매 부품 조회 완료 - 날짜: {}, 반환 수: {}", targetDate, parts.size());

        return com.stockmate.order.api.dashboard.dto.TopPartsResponseDTO.builder()
                .parts(parts)
                .build();
    }

    // 최근 주문 이력 조회
    @Transactional(readOnly = true)
    public RecentOrdersResponseDTO getRecentOrders(String date) {
        // 날짜 파싱 (미지정 시 오늘 날짜)
        LocalDate targetDate;
        if (date == null || date.isEmpty()) {
            targetDate = LocalDate.now();
            log.info("최근 주문 이력 조회 시작 - 날짜 미지정, 오늘 날짜 사용: {}", targetDate);
        } else {
            try {
                targetDate = LocalDate.parse(date);
                log.info("최근 주문 이력 조회 시작 - 지정된 날짜: {}", targetDate);
            } catch (Exception e) {
                log.error("날짜 파싱 실패 - 입력값: {}, 오늘 날짜로 대체", date);
                targetDate = LocalDate.now();
            }
        }

        // 해당 날짜의 시작/종료 시간 계산
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.atTime(23, 59, 59);

        log.info("최근 주문 이력 조회 기간 - 시작: {}, 종료: {}", startOfDay, endOfDay);

        // 최근 주문 조회 (최대 10개)
        List<Object[]> recentOrdersData = orderRepository.getRecentOrdersByDate(startOfDay, endOfDay);
        
        // 상위 10개만 선택
        List<Object[]> limitedData = recentOrdersData.stream()
                .limit(10)
                .collect(Collectors.toList());

        if (limitedData.isEmpty()) {
            log.info("최근 주문 이력 조회 완료 - 날짜: {}, 주문 수: 0", targetDate);
            return RecentOrdersResponseDTO.builder()
                    .orders(new ArrayList<>())
                    .build();
        }

        // 사용자 ID 목록 추출
        List<Long> memberIds = limitedData.stream()
                .map(row -> ((Number) row[4]).longValue()) // memberId
                .distinct()
                .collect(Collectors.toList());

        log.info("사용자 정보 조회 시작 - 회원 수: {}", memberIds.size());

        // 사용자 정보 일괄 조회
        Map<Long, UserBatchResponseDTO> userMap = userService.getUsersByMemberIds(memberIds);

        log.info("사용자 정보 조회 완료 - 조회된 회원 수: {}", userMap.size());

        // 주문 이력 DTO로 변환
        List<RecentOrdersResponseDTO.OrderInfo> orders = new ArrayList<>();
        for (Object[] row : limitedData) {
            Long orderId = ((Number) row[0]).longValue();
            LocalDateTime createdAt = (LocalDateTime) row[1];
            String orderNumber = (String) row[2];
            Integer totalPrice = ((Number) row[3]).intValue();
            Long memberId = ((Number) row[4]).longValue();
            Long totalItemQuantity = row[5] != null ? ((Number) row[5]).longValue() : 0L;

            // 사용자 이름 (storeName이 있으면 storeName, 없으면 owner)
            UserBatchResponseDTO user = userMap.get(memberId);
            String userName = "알 수 없는 가맹점";
            if (user != null) {
                userName = user.getStoreName() != null && !user.getStoreName().isEmpty() 
                        ? user.getStoreName() 
                        : (user.getOwner() != null ? user.getOwner() : "알 수 없는 가맹점");
            }

            orders.add(RecentOrdersResponseDTO.OrderInfo.builder()
                    .createdAt(createdAt)
                    .orderNumber(orderNumber)
                    .totalItemQuantity(totalItemQuantity.intValue())
                    .totalPrice(totalPrice)
                    .userName(userName)
                    .build());
        }

        log.info("최근 주문 이력 조회 완료 - 날짜: {}, 주문 수: {}", targetDate, orders.size());

        return RecentOrdersResponseDTO.builder()
                .orders(orders)
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

