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
public class TopPartsResponseDTO {
	private List<TopPart> parts; // 상위 판매 부품 목록 (최대 10개)

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class TopPart {
		private String name;          // 부품명 (OrderItem.name)
		private String categoryName;  // 카테고리명 (OrderItem.categoryName)
		private long salesCount;      // 판매 수량 합계
	}
}
