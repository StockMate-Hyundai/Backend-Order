package com.stockmate.order.api.cart.service;

import com.stockmate.order.api.cart.dto.*;
import com.stockmate.order.api.cart.entity.Cart;
import com.stockmate.order.api.cart.entity.CartItem;
import com.stockmate.order.api.cart.repository.CartRepository;
import com.stockmate.order.api.order.dto.PartDetailResponseDTO;
import com.stockmate.order.api.order.service.InventoryService;
import com.stockmate.order.common.exception.BadRequestException;
import com.stockmate.order.common.exception.NotFoundException;
import com.stockmate.order.common.response.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final InventoryService inventoryService;

    /**
     * 장바구니에 아이템 추가 (POST)
     * - 장바구니가 없으면 새로 생성
     * - 이미 있는 부품이면 수량 추가 (기존 수량 + 새 수량)
     * - 없는 부품이면 새로 추가
     */
    @Transactional
    public CartResponseDTO addToCart(AddToCartRequestDTO addToCartRequestDTO, Long memberId) {
        log.info("장바구니에 아이템 추가 - Member ID: {}, 아이템 수: {}", memberId, addToCartRequestDTO.getItems().size());

        // 입력 검증
        if (addToCartRequestDTO.getItems() == null || addToCartRequestDTO.getItems().isEmpty()) {
            throw new BadRequestException(ErrorStatus.VALIDATION_REQUEST_MISSING_EXCEPTION.getMessage());
        }

        // 회원의 장바구니 조회 (없으면 새로 생성)
        Cart cart = cartRepository.findByMemberIdWithItems(memberId)
                .orElseGet(() -> {
                    log.info("새 장바구니 생성 - Member ID: {}", memberId);
                    return Cart.builder()
                            .memberId(memberId)
                            .build();
                });

        // 각 아이템 추가 또는 수량 추가
        addToCartRequestDTO.getItems().forEach(item -> {
            if (item.getAmount() <= 0) {
                throw new BadRequestException(ErrorStatus.CART_AMOUNT_1_OVER_EXCEPTION.getMessage());
            }
            
            // 이미 존재하는 부품인지 확인
            CartItem existingItem = cart.getCartItems().stream()
                    .filter(cartItem -> cartItem.getPartId().equals(item.getPartId()))
                    .findFirst()
                    .orElse(null);

            if (existingItem != null) {
                // 이미 있으면 수량 추가
                int newAmount = existingItem.getAmount() + item.getAmount();
                existingItem.updateAmount(newAmount);
                log.debug("장바구니 아이템 수량 추가 - Part ID: {}, 기존: {}, 추가: {}, 새 수량: {}", 
                        item.getPartId(), existingItem.getAmount() - item.getAmount(), 
                        item.getAmount(), newAmount);
            } else {
                // 없으면 새로 추가
                CartItem newItem = CartItem.builder()
                        .cart(cart)
                        .partId(item.getPartId())
                        .amount(item.getAmount())
                        .build();
                cart.getCartItems().add(newItem);
                log.debug("장바구니 아이템 새로 추가 - Part ID: {}, Amount: {}", 
                        item.getPartId(), item.getAmount());
            }
        });

        Cart savedCart = cartRepository.save(cart);

        log.info("장바구니 저장 완료 - Cart ID: {}, Member ID: {}, 총 아이템 수: {}", savedCart.getCartId(), memberId, savedCart.getCartItems().size());
        return CartResponseDTO.from(savedCart);
    }

    /**
     * 장바구니 전체 수정 (PUT)
     * - 전체 데이터를 받아서 차이를 계산
     * - 기존에 있던 아이템이 없으면 삭제
     * - 새로운 아이템이 있으면 추가
     * - 수량이 달라지면 업데이트
     */
    @Transactional
    public CartResponseDTO updateCart(UpdateCartRequestDTO updateCartRequestDTO, Long memberId) {
        log.info("장바구니 전체 수정 - Member ID: {}, 새 아이템 수: {}", memberId, updateCartRequestDTO.getItems() != null ? updateCartRequestDTO.getItems().size() : 0);

        // 회원의 장바구니 조회
        Cart cart = cartRepository.findByMemberIdWithItems(memberId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.CART_EMPTY_EXCEPTION.getMessage()));

        // 입력 검증
        if (updateCartRequestDTO.getItems() == null) {
            updateCartRequestDTO = UpdateCartRequestDTO.builder().items(List.of()).build();
        }

        // 기존 아이템들의 partId 목록
        List<Long> existingPartIds = cart.getCartItems().stream()
                .map(CartItem::getPartId)
                .toList();

        // 새로운 아이템들의 partId 목록
        List<Long> newPartIds = updateCartRequestDTO.getItems().stream()
                .map(CartItemRequestDTO::getPartId)
                .toList();

        // 삭제할 아이템 (기존에는 있었는데 새 데이터에는 없음)
        List<Long> toRemove = existingPartIds.stream()
                .filter(partId -> !newPartIds.contains(partId))
                .toList();

        toRemove.forEach(partId -> {
            cart.removeItem(partId);
            log.debug("장바구니 아이템 삭제 - Part ID: {}", partId);
        });

        // 추가 또는 업데이트
        updateCartRequestDTO.getItems().forEach(item -> {
            if (item.getAmount() <= 0) {
                throw new BadRequestException(ErrorStatus.CART_AMOUNT_1_OVER_EXCEPTION.getMessage());
            }
            cart.addOrUpdateItem(item.getPartId(), item.getAmount());
            log.debug("장바구니 아이템 추가/업데이트 - Part ID: {}, Amount: {}", 
                    item.getPartId(), item.getAmount());
        });

        Cart savedCart = cartRepository.save(cart);

        log.info("장바구니 전체 수정 완료 - Cart ID: {}, Member ID: {}, 총 아이템 수: {}", 
                savedCart.getCartId(), memberId, savedCart.getCartItems().size());

        return CartResponseDTO.from(savedCart);
    }

    // 내 장바구니 조회
    @Transactional(readOnly = true)
    public CartDetailResponseDTO getMyCart(Long memberId) {
        log.info("장바구니 조회 - Member ID: {}", memberId);

        Cart cart = cartRepository.findByMemberIdWithItems(memberId).orElse(null);

        if (cart == null || cart.getCartItems().isEmpty()) {
            log.info("장바구니가 없거나 비어있음 - Member ID: {}, 빈 장바구니 반환", memberId);
            // 장바구니가 없으면 빈 장바구니 반환
            return CartDetailResponseDTO.builder()
                    .memberId(memberId)
                    .items(List.of())
                    .totalPrice(0)
                    .build();
        }

        // 부품 ID 목록 추출
        List<Long> partIds = cart.getCartItems().stream()
                .map(CartItem::getPartId)
                .distinct()
                .collect(Collectors.toList());

        // 부품 상세 정보 조회 (InventoryService 사용)
        Map<Long, PartDetailResponseDTO> partDetailsMap = inventoryService.getPartDetails(partIds);

        // CartItem + 부품 상세 정보 결합
        List<CartItemDetailResponseDTO> itemDetails = new ArrayList<>();
        int totalPrice = 0;

        for (CartItem cartItem : cart.getCartItems()) {
            PartDetailResponseDTO partDetail = partDetailsMap.get(cartItem.getPartId());
            
            if (partDetail == null) {
                log.warn("부품 정보를 찾을 수 없음 - Part ID: {}", cartItem.getPartId());
                continue; // 부품 정보가 없으면 스킵
            }

            CartItemDetailResponseDTO itemDetail = CartItemDetailResponseDTO.builder()
                    .cartItemId(cartItem.getCartItemId())
                    .partId(cartItem.getPartId())
                    .amount(cartItem.getAmount())
                    .partName(partDetail.getName())
                    .categoryName(partDetail.getCategoryName())
                    .brand(partDetail.getKorName())
                    .model(partDetail.getModel())
                    .trim(partDetail.getTrim())
                    .price(partDetail.getPrice())
                    .stock(partDetail.getAmount())
                    .build();

            itemDetails.add(itemDetail);
            totalPrice += partDetail.getPrice() * cartItem.getAmount();
        }

        log.info("장바구니 조회 완료 - Cart ID: {}, 아이템 수: {}, 총 금액: {}", 
                cart.getCartId(), itemDetails.size(), totalPrice);

        return CartDetailResponseDTO.builder()
                .cartId(cart.getCartId())
                .memberId(cart.getMemberId())
                .items(itemDetails)
                .totalPrice(totalPrice)
                .build();
    }

    // 장바구니 전체 비우기
    @Transactional
    public void clearCart(Long memberId) {
        log.info("장바구니 전체 비우기 - Member ID: {}", memberId);

        Cart cart = cartRepository.findByMemberIdWithItems(memberId)
                .orElseThrow(() -> new BadRequestException(ErrorStatus.CART_EMPTY_EXCEPTION.getMessage()));

        cart.clearItems();
        cartRepository.save(cart);

        log.info("장바구니 비우기 완료 - Member ID: {}", memberId);
    }
}

