package com.stockmate.order.api.order.service.fallback;

import com.stockmate.order.api.order.dto.InventoryCheckResponseDTO;
import com.stockmate.order.api.order.dto.OrderItemCheckRequestDTO;
import com.stockmate.order.api.order.dto.PartDetailResponseDTO;
import com.stockmate.order.common.exception.InternalServerException;
import com.stockmate.order.common.response.ErrorStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class InventoryServiceFallback {

    /**
     * 부품 재고 체크 Fallback
     * - 주문 생성 시 필수이므로 예외를 발생시킴
     */
    public InventoryCheckResponseDTO checkInventoryFallback(List<OrderItemCheckRequestDTO> orderItems, Exception e) {
        log.error("부품 재고 체크 Circuit Breaker 작동 - Fallback 실행. Error: {}", e.getMessage());
        throw new InternalServerException(ErrorStatus.PARTS_SERVER_UNAVAILABLE_EXCEPTION.getMessage());
    }

    /**
     * 부품 상세 정보 조회 Fallback
     * - 주문 리스트 조회 시 부품 정보가 없으면 예외 발생
     */
    public Map<Long, PartDetailResponseDTO> getPartDetailsFallback(List<Long> partIds, Exception e) {
        log.error("부품 상세 정보 조회 Circuit Breaker 작동 - Fallback 실행. Error: {}", e.getMessage());
        throw new InternalServerException(ErrorStatus.PARTS_SERVER_UNAVAILABLE_EXCEPTION.getMessage());
    }
}
