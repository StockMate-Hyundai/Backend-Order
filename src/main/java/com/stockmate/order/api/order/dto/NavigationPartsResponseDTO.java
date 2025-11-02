package com.stockmate.order.api.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "네비게이션용 부품 정보 응답 DTO")
public class NavigationPartsResponseDTO {
    
    @Schema(description = "부품 위치 리스트")
    private List<PartLocationInfo> partLocations;
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "부품 위치 정보")
    public static class PartLocationInfo {
        @Schema(description = "부품 ID", example = "1")
        private Long partId;
        
        @Schema(description = "부품 이름", example = "브레이크 패드")
        private String partName;
        
        @Schema(description = "부품 위치", example = "A5-2")
        private String location;
        
        @Schema(description = "주문 번호", example = "SMO-1")
        private String orderNumber;
        
        @Schema(description = "주문 수량", example = "2")
        private Integer quantity;
    }
}

