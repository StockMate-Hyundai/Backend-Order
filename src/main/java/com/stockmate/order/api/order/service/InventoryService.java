package com.stockmate.order.api.order.service;

import com.stockmate.order.api.order.dto.InventoryCheckApiResponse;
import com.stockmate.order.api.order.dto.InventoryCheckResponseDTO;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final WebClient webClient;

    @Value("${inventory.server.url}")
    private String inventoryServerUrl;

    public void checkInventory(Long partId, Integer amount) {
        log.info("부품 재고 체크 요청 - Part ID: {}, Amount: {}", partId, amount);

        try {
            InventoryCheckApiResponse response = webClient.get()
                    .uri(inventoryServerUrl + "/api/v1/inventory/check?partId={partId}&amount={amount}", partId, amount)
                    .retrieve()
                    .bodyToMono(InventoryCheckApiResponse.class)
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        log.error("부품 재고 체크 API 호출 실패 - Part ID: {}, Amount: {}, Status: {}, Response: {}", 
                                partId, amount, ex.getStatusCode(), ex.getResponseBodyAsString());
                        return Mono.error(new InternalServerException(ErrorStatus.NOT_CONNECTTION_PARTS_STOCK_EXCEPTION.getMessage()));
                    })
                    .onErrorResume(Exception.class, ex -> {
                        log.error("부품 재고 체크 중 예외 발생 - Part ID: {}, Amount: {}, Error: {}", 
                                partId, amount, ex.getMessage(), ex);
                        return Mono.error(new InternalServerException(ErrorStatus.CHECK_PARTS_STOCK_EXCEPTION.getMessage()));
                    })
                    .block();

            if (response == null || !response.isSuccess()) {
                log.error("부품 재고 체크 응답 실패 - Part ID: {}, Amount: {}, Response: {}", 
                        partId, amount, response);
                throw new InternalServerException(ErrorStatus.RESPONSE_DATA_NOT_MATCH_EXCEPTION.getMessage());
            }

            InventoryCheckResponseDTO data = response.getData();
            if (data == null) {
                log.error("부품 재고 체크 데이터가 null - Part ID: {}, Amount: {}", partId, amount);
                throw new InternalServerException(ErrorStatus.RESPONSE_DATA_NULL_EXCEPTION.getMessage());
            }

            log.info("부품 재고 체크 완료 - Part ID: {}, Stock: {}, CanOrder: {}", 
                    data.getPartId(), data.getStock(), data.isCanOrder());

            // 주문 불가능한 경우 예외 처리
            if (!data.isCanOrder()) {
                log.warn("부품 주문 불가능 - Part ID: {}, 현재 재고: {}, 요청 수량: {}", 
                        partId, data.getStock(), amount);
                throw new BadRequestException(
                    String.format("재고가 부족합니다. 현재 본사 재고: %d개, 요청 수량: %d개", 
                            data.getStock(), amount)
                );
            }

            // 재고 체크 성공

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("부품 재고 체크 중 예상치 못한 오류 - Part ID: {}, Amount: {}, Error: {}", 
                    partId, amount, e.getMessage(), e);
            throw new InternalServerException(ErrorStatus.CHECK_PARTS_STOCK_EXCEPTION.getMessage());
        }
    }
}
