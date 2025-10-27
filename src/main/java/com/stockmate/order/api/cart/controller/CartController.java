package com.stockmate.order.api.cart.controller;

import com.stockmate.order.api.cart.dto.*;
import com.stockmate.order.api.cart.service.CartService;
import com.stockmate.order.common.config.security.SecurityUser;
import com.stockmate.order.common.response.ApiResponse;
import com.stockmate.order.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Cart", description = "장바구니 관련 API 입니다.")
@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;

    @Operation(summary = "장바구니 등록 API", description = "장바구니를 등록합니다. 이미 장바구니가 존재하면 기존 장바구니 ID를 반환합니다.")
    @PostMapping("/cart")
    public ResponseEntity<ApiResponse<CartResponseDTO>> addToCart(@RequestBody AddToCartRequestDTO addToCartRequestDTO, @AuthenticationPrincipal SecurityUser securityUser) {

        log.info("장바구니 등록 요청 - Member ID: {}, 아이템 수: {}", securityUser.getMemberId(), addToCartRequestDTO.getItems().size());
        CartResponseDTO response = cartService.addToCart(addToCartRequestDTO, securityUser.getMemberId());

        log.info("장바구니 등록 완료 - Cart ID: {}, 총 아이템 수: {}", response.getCartId(), response.getItems().size());
        return ApiResponse.success(SuccessStatus.SEND_CART_CREATE_SUCCESS, response);
    }

    @Operation(summary = "장바구니 수정 API", description = "장바구니를 수정합니다. 기존 데이터와 비교하여 추가/삭제/수정을 처리합니다.")
    @PutMapping("/cart")
    public ResponseEntity<ApiResponse<CartResponseDTO>> updateCart(@RequestBody UpdateCartRequestDTO updateCartRequestDTO, @AuthenticationPrincipal SecurityUser securityUser) {

        log.info("장바구니 전체 수정 요청 - Member ID: {}, 새 아이템 수: {}", securityUser.getMemberId(), updateCartRequestDTO.getItems() != null ? updateCartRequestDTO.getItems().size() : 0);
        CartResponseDTO response = cartService.updateCart(updateCartRequestDTO, securityUser.getMemberId());

        log.info("장바구니 전체 수정 완료 - Cart ID: {}, 총 아이템 수: {}", response.getCartId(), response.getItems().size());
        return ApiResponse.success(SuccessStatus.SEND_CART_MODIFY_SUCCESS, response);
    }

    @Operation(summary = "내 장바구니 조회 API", description = "내 장바구니와 담긴 부품 목록을 부품 상세 정보와 함께 조회합니다.")
    @GetMapping("/cart")
    public ResponseEntity<ApiResponse<CartDetailResponseDTO>> getMyCart(@AuthenticationPrincipal SecurityUser securityUser) {

        log.info("장바구니 조회 요청 - Member ID: {}", securityUser.getMemberId());
        CartDetailResponseDTO response = cartService.getMyCart(securityUser.getMemberId());

        log.info("장바구니 조회 완료 - 아이템 수: {}, 총 금액: {}", response.getItems().size(), response.getTotalPrice());
        return ApiResponse.success(SuccessStatus.SEND_CART_DATA_SUCCESS, response);
    }

    @Operation(summary = "장바구니 특정 부품 삭제 API", description = "장바구니에서 특정 부품을 삭제합니다.")
    @DeleteMapping("/cart/item/{partId}")
    public ResponseEntity<ApiResponse<Void>> removeItemFromCart(
            @PathVariable Long partId,
            @AuthenticationPrincipal SecurityUser securityUser) {

        log.info("장바구니 특정 부품 삭제 요청 - Member ID: {}, Part ID: {}", securityUser.getMemberId(), partId);
        cartService.removeItemFromCart(securityUser.getMemberId(), partId);

        log.info("장바구니 특정 부품 삭제 완료 - Member ID: {}, Part ID: {}", securityUser.getMemberId(), partId);
        return ApiResponse.success_only(SuccessStatus.SEND_CART_DELETE_SUCCESS);
    }

    @Operation(summary = "장바구니 전체 비우기 API", description = "장바구니의 모든 아이템을 삭제합니다.")
    @DeleteMapping("/cart")
    public ResponseEntity<ApiResponse<Void>> clearCart(@AuthenticationPrincipal SecurityUser securityUser) {

        log.info("장바구니 전체 비우기 요청 - Member ID: {}", securityUser.getMemberId());
        cartService.clearCart(securityUser.getMemberId());

        log.info("장바구니 전체 비우기 완료 - Member ID: {}", securityUser.getMemberId());
        return ApiResponse.success_only(SuccessStatus.SEND_CART_DELETE_SUCCESS);
    }
}
