package com.stockmate.order.api.order.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.stockmate.order.api.order.entity.Order;
import com.stockmate.order.api.order.entity.OrderStatus;
import com.stockmate.order.api.order.entity.QOrder;
import com.stockmate.order.api.order.entity.QOrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Order> findOrdersWithFilters(
            OrderStatus status,
            Long partId,
            Long memberId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {

        QOrder order = QOrder.order;
        QOrderItem orderItem = QOrderItem.orderItem;

        // 1단계: 카운트 쿼리 (전체 개수 조회)
        Long total = queryFactory
                .select(order.orderId.countDistinct())
                .from(order)
                .leftJoin(order.orderItems, orderItem)
                .where(
                        statusEq(status),
                        memberIdEq(memberId),
                        partIdEq(partId, orderItem),
                        createdAtBetween(startDate, endDate)
                )
                .fetchOne();

        if (total == null || total == 0) {
            return new PageImpl<>(new ArrayList<>(), pageable, 0);
        }

        // 2단계: ID만 페이징해서 조회
        List<Long> orderIds = queryFactory
                .select(order.orderId)
                .from(order)
                .leftJoin(order.orderItems, orderItem)
                .where(
                        statusEq(status),
                        memberIdEq(memberId),
                        partIdEq(partId, orderItem),
                        createdAtBetween(startDate, endDate)
                )
                .orderBy(order.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .distinct()
                .fetch();

        if (orderIds.isEmpty()) {
            return new PageImpl<>(new ArrayList<>(), pageable, total);
        }

        // 3단계: ID로 실제 엔티티 + 컬렉션 fetch join
        List<Order> orders = queryFactory
                .selectFrom(order)
                .leftJoin(order.orderItems, orderItem).fetchJoin()
                .where(order.orderId.in(orderIds))
                .orderBy(order.createdAt.desc())
                .fetch();

        return new PageImpl<>(orders, pageable, total);
    }

    // 상태 필터
    private BooleanExpression statusEq(OrderStatus status) {
        return status != null ? QOrder.order.orderStatus.eq(status) : null;
    }

    // 회원 ID 필터
    private BooleanExpression memberIdEq(Long memberId) {
        return memberId != null ? QOrder.order.memberId.eq(memberId) : null;
    }

    // 부품 ID 필터
    private BooleanExpression partIdEq(Long partId, QOrderItem orderItem) {
        return partId != null ? orderItem.partId.eq(partId) : null;
    }

    // 날짜 범위 필터
    private BooleanExpression createdAtBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
            return QOrder.order.createdAt.between(startDateTime, endDateTime);
        } else if (startDate != null) {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            return QOrder.order.createdAt.goe(startDateTime);
        } else if (endDate != null) {
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
            return QOrder.order.createdAt.loe(endDateTime);
        }
        return null;
    }
}
