package com.stockmate.order.api.order.service.fallback;

import com.stockmate.order.api.order.dto.UserBatchResponseDTO;
import com.stockmate.order.common.exception.InternalServerException;
import com.stockmate.order.common.response.ErrorStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class UserServiceFallback {

    /**
     * 사용자 정보 조회 Fallback
     * - 주문 리스트 조회 시 사용자 정보가 없으면 예외 발생
     */
    public Map<Long, UserBatchResponseDTO> getUsersByMemberIdsFallback(List<Long> memberIds, Exception e) {
        log.error("사용자 정보 조회 Circuit Breaker 작동 - Fallback 실행. Error: {}", e.getMessage());
        throw new InternalServerException(ErrorStatus.USER_SERVER_UNAVAILABLE_EXCEPTION.getMessage());
    }
}
