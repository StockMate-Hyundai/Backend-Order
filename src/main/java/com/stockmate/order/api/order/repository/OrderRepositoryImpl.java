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

        // QueryDSL 쿼리 빌드
        var query = queryFactory
                .selectFrom(order)
                .leftJoin(order.orderItems, orderItem).fetchJoin()
                .where(
                        statusEq(status),
                        memberIdEq(memberId),
                        partIdEq(partId, orderItem),
                        createdAtBetween(startDate, endDate)
                )
                .orderBy(order.createdAt.desc())
                .distinct();

        // 전체 개수 조회
        long total = query.fetch().size();

        // 페이징 처리
        List<Order> orders = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
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
