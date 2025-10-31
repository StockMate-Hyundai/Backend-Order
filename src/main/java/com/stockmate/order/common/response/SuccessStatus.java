package com.stockmate.order.common.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public enum SuccessStatus {

	/**
	 * 200
	 */
	SEND_HEALTH_CHECK_SUCCESS(HttpStatus.OK,"서버 상태 체크 성공"),
	SEND_ORDER_APPROVAL_REQUEST_SUCCESS(HttpStatus.OK, "주문 승인 요청 성공"),
	CHECK_ORDER_APPROVAL_STATUS_SUCCESS(HttpStatus.OK, "주문 상태 조회 성공"),
	SEND_CANCELLED_ORDER_SUCCESS(HttpStatus.OK, "주문 취소 성공"),
	DELETE_ORDER_SUCCESS(HttpStatus.OK, "주문 삭제 성공"),
	SEND_MY_ORDER_LIST_SUCCESS(HttpStatus.OK, "내 주문 리스트 조회 성공"),
	SEND_ORDER_LIST_SUCCESS(HttpStatus.OK, "주문 리스트 조회 성공"),
	SEND_ORDER_DETAIL_SUCCESS(HttpStatus.OK, "주문 상세 조회 성공"),
	SEND_ORDER_REJECT_REQUEST_SUCCESS(HttpStatus.OK,"주문 반려 성공"),
	SEND_CART_MODIFY_SUCCESS(HttpStatus.OK,"장바구니 수정 성공"),
	SEND_CART_DELETE_SUCCESS(HttpStatus.OK,"장바구니 삭제 성공"),
	SEND_CART_DATA_SUCCESS(HttpStatus.OK,"장바구니 조회 성공"),
	REGISTER_SHIPPING_SUCCESS(HttpStatus.OK,"배송 등록 성공"),
	REQUEST_RECEIVING_PROCESS_SUCCESS(HttpStatus.OK,"입고 처리 요청 성공"),
    CHECK_ORDER_DATA_SUCCESS(HttpStatus.OK, "주문 검증 조회 성공"),
    GET_TODAY_DASHBOARD_SUCCESS(HttpStatus.OK, "금일 대시보드 조회 성공"),
    GET_CATEGORY_SALES_SUCCESS(HttpStatus.OK, "카테고리별 판매량 조회 성공"),
    GET_RECENT_ORDERS_SUCCESS(HttpStatus.OK, "최근 주문 이력 조회 성공"),
	GET_TODAY_INOUT_DASHBOARD_SUCCESS(HttpStatus.OK, "금일 시간대별 입출고 조회 성공"),
	GET_TOP_PARTS_SUCCESS(HttpStatus.OK, "TOP 판매 부품 조회 성공"),

	/**
	 * 201
	 */
	SEND_PARTS_ORDER_SUCCESS(HttpStatus.CREATED,"부품 주문 성공"),
	SEND_CART_CREATE_SUCCESS(HttpStatus.CREATED, "장바구니 생성 성공"),

	;

	private final HttpStatus httpStatus;
	private final String message;

	public int getStatusCode() {
		return this.httpStatus.value();
	}
}