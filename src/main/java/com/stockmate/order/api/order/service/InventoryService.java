package com.stockmate.order.api.order.service;

import com.stockmate.order.api.order.dto.*;
import com.stockmate.order.common.exception.BadRequestException;
import com.stockmate.order.common.exception.InternalServerException;
import com.stockmate.order.common.response.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final WebClient webClient;

    @Value("${inventory.server.url}")
    private String inventoryServerUrl;

    public InventoryCheckResponseDTO checkInventory(List<OrderItemCheckRequestDTO> orderItems) {
        log.info("부품 재고 체크 요청 - 주문 항목 수: {}", orderItems.size());

        try {
            InventoryCheckApiResponse response = webClient.post()
                    .uri(inventoryServerUrl + "/api/v1/parts/check")
                    .bodyValue(orderItems)
                    .retrieve()
                    .bodyToMono(InventoryCheckApiResponse.class)
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        log.error("부품 재고 체크 API 호출 실패 - Status: {}, Response: {}", 
                                ex.getStatusCode(), ex.getResponseBodyAsString());
                        return Mono.error(new InternalServerException(ErrorStatus.NOT_CONNECTTION_PARTS_STOCK_EXCEPTION.getMessage()));
                    })
                    .onErrorResume(Exception.class, ex -> {
                        log.error("부품 재고 체크 중 예외 발생 - Error: {}", ex.getMessage(), ex);
                        return Mono.error(new InternalServerException(ErrorStatus.CHECK_PARTS_STOCK_EXCEPTION.getMessage()));
                    })
                    .block();

            if (response == null || !response.isSuccess()) {
                log.error("부품 재고 체크 응답 실패 - Response: {}", response);
                throw new InternalServerException(ErrorStatus.RESPONSE_DATA_NOT_MATCH_EXCEPTION.getMessage());
            }

            InventoryCheckResponseDTO data = response.getData();
            if (data == null || data.getData() == null) {
                log.error("부품 재고 체크 데이터가 null");
                throw new InternalServerException(ErrorStatus.RESPONSE_DATA_NULL_EXCEPTION.getMessage());
            }

            log.info("부품 재고 체크 완료 - 총 금액: {}, 체크 항목 수: {}", 
                    data.getTotalAmount(), data.getData().size());

            // 주문 불가능한 부품이 있는지 확인
            List<InventoryCheckItemResponseDTO> checkResults = data.getData();
            for (InventoryCheckItemResponseDTO item : checkResults) {
                if (!item.isCanOrder()) {
                    log.warn("부품 주문 불가능 - Part ID: {}, 요청 수량: {}, 현재 재고: {}", 
                            item.getPartId(), item.getRequestedAmount(), item.getAvailableStock());
                    throw new BadRequestException(ErrorStatus.SOLD_OUT_PARTS_EXCEPTION.getMessage(), data);
                }
            }

            log.info("모든 부품 주문 가능 확인 완료");
            return data;

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("부품 재고 체크 중 예상치 못한 오류 - Error: {}", e.getMessage(), e);
            throw new InternalServerException(ErrorStatus.CHECK_PARTS_STOCK_EXCEPTION.getMessage());
        }
    }
}
