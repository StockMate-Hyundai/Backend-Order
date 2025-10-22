package com.stockmate.order.api.cart.repository;

import com.stockmate.order.api.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    
    // 회원 ID로 장바구니 조회
    Optional<Cart> findByMemberId(Long memberId);
    
    // 회원 ID로 장바구니와 아이템 함께 조회 (fetch join)
    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.cartItems WHERE c.memberId = :memberId")
    Optional<Cart> findByMemberIdWithItems(@Param("memberId") Long memberId);
}

