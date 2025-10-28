package com.stockmate.order.api.order.service;

import com.stockmate.order.api.order.dto.*;
import com.stockmate.order.api.order.service.fallback.InventoryServiceFallback;
import com.stockmate.order.common.exception.BadRequestException;
import com.stockmate.order.common.exception.InternalServerException;
import com.stockmate.order.common.response.ErrorStatus;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final WebClient webClient;
    private final InventoryServiceFallback inventoryServiceFallback;

    @Value("${inventory.server.url}")
    private String inventoryServerUrl;

    // @CircuitBreaker(name = "partsService", fallbackMethod = "checkInventoryFallback")
    public InventoryCheckResponseDTO checkInventory(List<OrderItemCheckRequestDTO> orderItems) {
        log.info("부품 재고 체크 요청 - 주문 항목 수: {}", orderItems.size());

        try {
            InventoryCheckApiResponse response = webClient.post()
                    .uri(inventoryServerUrl + "/api/v1/parts/check")
                    .bodyValue(orderItems)
                    .retrieve()
                    .bodyToMono(InventoryCheckApiResponse.class)
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        // 4xx 에러는 그대로 전달 (비즈니스 예외)
                        if (ex.getStatusCode().is4xxClientError()) {
                            log.warn("부품 재고 체크 클라이언트 에러 - Status: {}, Response: {}", 
                                    ex.getStatusCode(), ex.getResponseBodyAsString());
                            return Mono.error(ex);  // 그대로 전달
                        }
                        // 5xx 에러만 InternalServerException으로 변환
                        log.error("부품 재고 체크 서버 에러 - Status: {}, Response: {}", 
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
            if (data == null || data.getOrderList() == null || data.getOrderList().isEmpty()) {
                log.error("부품 재고 체크 데이터가 null 또는 비어있음");
                throw new InternalServerException(ErrorStatus.RESPONSE_DATA_NULL_EXCEPTION.getMessage());
            }

            List<InventoryCheckItemResponseDTO> checkResults = data.getOrderList();
            int totalPrice = data.getTotalPrice();

            log.info("부품 재고 체크 완료 - 체크 항목 수: {}, 총 금액: {}", checkResults.size(), totalPrice);

            // 주문 불가능한 부품이 있는지 확인
            for (InventoryCheckItemResponseDTO item : checkResults) {
                if (!item.isCanOrder()) {
                    log.warn("부품 주문 불가능 - Part ID: {}, 요청 수량: {}, 현재 재고: {}", 
                            item.getPartId(), item.getRequestedAmount(), item.getAvailableStock());
                    
                    // 에러 응답용 DTO 생성
                    throw new BadRequestException(ErrorStatus.SOLD_OUT_PARTS_EXCEPTION.getMessage(), data);
                }
            }

            log.info("모든 부품 주문 가능 확인 완료");
            
            // 성공 응답 반환
            return data;

        } catch (BadRequestException e) {
            throw e;
        } catch (WebClientResponseException e) {
            // WebClient 예외는 그대로 던져서 Circuit Breaker가 판단하도록
            throw e;
        } catch (Exception e) {
            log.error("부품 재고 체크 중 예상치 못한 오류 - Error: {}", e.getMessage(), e);
            throw new InternalServerException(ErrorStatus.CHECK_PARTS_STOCK_EXCEPTION.getMessage());
        }
    }

    // @CircuitBreaker(name = "partsService", fallbackMethod = "getPartDetailsFallback")
    public Map<Long, PartDetailResponseDTO> getPartDetails(List<Long> partIds) {
        log.info("부품 상세 정보 일괄 조회 요청 - Part IDs 수: {}", partIds.size());

        try {
            PartDetailApiResponse response = webClient.post()
                    .uri(inventoryServerUrl + "/api/v1/parts/detail")
                    .bodyValue(partIds)
                    .retrieve()
                    .bodyToMono(PartDetailApiResponse.class)
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        // 4xx 에러는 그대로 전달 (비즈니스 예외)
                        if (ex.getStatusCode().is4xxClientError()) {
                            log.warn("부품 상세 정보 조회 클라이언트 에러 - Status: {}, Response: {}", 
                                    ex.getStatusCode(), ex.getResponseBodyAsString());
                            return Mono.error(ex);  // 그대로 전달
                        }
                        // 5xx 에러만 InternalServerException으로 변환
                        log.error("부품 상세 정보 조회 서버 에러 - Status: {}, Response: {}",
                                ex.getStatusCode(), ex.getResponseBodyAsString());
                        return Mono.error(new InternalServerException(ErrorStatus.NOT_CONNECTTION_PARTS_DETAIL_EXCEPTION.getMessage()));
                    })
                    .onErrorResume(Exception.class, ex -> {
                        log.error("부품 상세 정보 조회 중 예외 발생 - Error: {}", ex.getMessage(), ex);
                        return Mono.error(new InternalServerException(ErrorStatus.CHECK_PARTS_DETAIL_EXCEPTION.getMessage()));
                    })
                    .block();

            if (response == null || !response.isSuccess() || response.getData() == null) {
                log.error("부품 상세 정보 조회 응답 실패");
                throw new InternalServerException(ErrorStatus.RESPONSE_DATA_NOT_MATCH_EXCEPTION.getMessage());
            }

            // List를 Map으로 변환 (partId를 key로)
            Map<Long, PartDetailResponseDTO> partMap = new HashMap<>();
            for (PartDetailResponseDTO part : response.getData()) {
                partMap.put(part.getId(), part);
            }

            log.info("부품 상세 정보 일괄 조회 완료 - 조회된 부품 수: {}", partMap.size());
            return partMap;

        } catch (InternalServerException e) {
            throw e;
        } catch (WebClientResponseException e) {
            // WebClient 예외는 그대로 던져서 Circuit Breaker가 판단하도록
            throw e;
        } catch (Exception e) {
            log.error("부품 상세 정보 조회 중 예상치 못한 오류 - Error: {}", e.getMessage(), e);
            throw new InternalServerException(ErrorStatus.CHECK_PARTS_DETAIL_EXCEPTION.getMessage());
        }
    }

    // 본사 재고 차감 (주문 승인용)
    public void deductStock(Long orderId, String orderNumber, List<Map<String, Object>> items) {
        log.info("Parts 서버 재고 차감 API 호출 - Order ID: {}, Order Number: {}, 아이템 수: {}", orderId, orderNumber, items.size());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("orderId", orderId);
        requestBody.put("orderNumber", orderNumber);
        requestBody.put("items", items);

        try {
            String response = webClient.post()
                    .uri(inventoryServerUrl + "/api/v1/parts/deduct-stock")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Parts 서버 재고 차감 성공 - 응답: {}", response);
            
        } catch (WebClientResponseException e) {
            log.error("Parts 서버 재고 차감 실패 - Status: {}, Response: {}", 
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new InternalServerException("Parts 서버 재고 차감 실패: " + e.getMessage());
        } catch (Exception e) {
            log.error("Parts 서버 재고 차감 중 예외 발생 - Error: {}", e.getMessage(), e);
            throw new InternalServerException("Parts 서버 재고 차감 실패: " + e.getMessage());
        }
    }

    // 가맹점 부품 재고 업데이트 (입고 처리)
    public void updateStoreInventory(Long memberId, List<Map<String, Object>> items) {
        log.info("Parts 서버 재고 업데이트 API 호출 - 가맹점 ID: {}, 아이템 수: {}", memberId, items.size());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("memberId", memberId);
        requestBody.put("items", items);

        try {
            String response = webClient.post()
                    .uri(inventoryServerUrl + "/api/v1/store/inventory/update")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Parts 서버 재고 업데이트 성공 - 응답: {}", response);
            
        } catch (WebClientResponseException e) {
            log.error("Parts 서버 재고 업데이트 실패 - Status: {}, Response: {}", 
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new InternalServerException("Parts 서버 재고 업데이트 실패: " + e.getMessage());
        } catch (Exception e) {
            log.error("Parts 서버 재고 업데이트 중 예외 발생 - Error: {}", e.getMessage(), e);
            throw new InternalServerException("Parts 서버 재고 업데이트 실패: " + e.getMessage());
        }
    }

    // Circuit Breaker Fallback 메서드들
    // private InventoryCheckResponseDTO checkInventoryFallback(List<OrderItemCheckRequestDTO> orderItems, Exception e) {
    //     return inventoryServiceFallback.checkInventoryFallback(orderItems, e);
    // }

    // private Map<Long, PartDetailResponseDTO> getPartDetailsFallback(List<Long> partIds, Exception e) {
    //     return inventoryServiceFallback.getPartDetailsFallback(partIds, e);
    // }
}
