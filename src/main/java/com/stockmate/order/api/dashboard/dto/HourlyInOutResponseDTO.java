package com.stockmate.order.api.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HourlyInOutResponseDTO {
	private List<HourStat> hours; // 0~23

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class HourStat {
		private int hour;              // 0-23
		private long inboundOrders;    // 해당 시간대 생성된 주문 수
		private long outboundShipped;  // 해당 시간대 배송 처리 수
	}
}
