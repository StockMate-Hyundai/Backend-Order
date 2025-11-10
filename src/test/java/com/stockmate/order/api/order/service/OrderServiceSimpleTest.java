package com.stockmate.order.api.order.service;

import com.stockmate.order.api.notification.service.ApplicationNotificationService;
import com.stockmate.order.api.order.dto.*;
import com.stockmate.order.api.order.entity.Order;
import com.stockmate.order.api.order.entity.OrderItem;
import com.stockmate.order.api.order.entity.OrderStatus;
import com.stockmate.order.api.order.entity.PaymentType;
import com.stockmate.order.api.order.repository.OrderRepository;
import com.stockmate.order.api.websocket.handler.DashboardWebSocketHandler;
import com.stockmate.order.api.websocket.handler.OrderWebSocketHandler;
import com.stockmate.order.common.config.security.Role;
import com.stockmate.order.common.producer.KafkaProducerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 간단 테스트")
class OrderServiceSimpleTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private UserService userService;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private OrderTransactionService orderTransactionService;

    @Mock
    private OrderWebSocketHandler orderWebSocketHandler;

    @Mock
    private DashboardWebSocketHandler dashboardWebSocketHandler;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private ApplicationNotificationService applicationNotificationService;

    @InjectMocks
    private OrderService orderService;

    @InjectMocks
    private PaymentService paymentService;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
                .orderId(1L)
                .orderNumber("SMO-1")
                .totalPrice(150000)
                .paymentType(PaymentType.CARD)
                .requestedShippingDate(LocalDate.now().plusDays(1))
                .orderStatus(OrderStatus.ORDER_COMPLETED)
                .memberId(1L)
                .etc("테스트 주문")
                .orderItems(new ArrayList<>())
                .build();

        OrderItem orderItem = OrderItem.builder()
                .id(1L)
                .order(testOrder)
                .partId(101L)
                .amount(2)
                .categoryName("엔진부품")
                .name("에어필터")
                .price(50000L)
                .cost(30000L)
                .location("A1-1")
                .build();

        testOrder.getOrderItems().add(orderItem);
    }

    @Test
    @DisplayName("주문 생성 성공 테스트")
    void makeOrder_Success() {
        // given
        Long memberId = 1L;
        
        OrderItemRequestDTO itemDTO = OrderItemRequestDTO.builder()
                .partId(101L)
                .amount(2)
                .build();

        OrderRequestDTO orderRequestDTO = OrderRequestDTO.builder()
                .orderItems(List.of(itemDTO))
                .paymentType(PaymentType.CARD)
                .requestedShippingDate(LocalDate.now().plusDays(1))
                .etc("테스트 주문")
                .build();

        InventoryCheckItemResponseDTO itemResponse = InventoryCheckItemResponseDTO.builder()
                .partId(101L)
                .categoryName("엔진부품")
                .name("에어필터")
                .price(50000L)
                .cost(30000L)
                .location("A1-1")
                .build();

        InventoryCheckResponseDTO inventoryCheckResponse = InventoryCheckResponseDTO.builder()
                .totalPrice(100000)
                .orderList(List.of(itemResponse))
                .build();
        
        given(inventoryService.checkInventory(anyList())).willReturn(inventoryCheckResponse);
        given(orderRepository.save(any(Order.class))).willReturn(testOrder);

        // when
        MakeOrderResponseDto response = orderService.makeOrder(orderRequestDTO, memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(1L);
        assertThat(response.getOrderNumber()).isEqualTo("SMO-1");
        
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(applicationEventPublisher, times(1)).publishEvent(any(PayRequestEvent.class));
    }

//    @Test
//    @DisplayName("주문 취소 성공 테스트 - 관리자")
//    void cancelOrder_Success_Admin() {
//        // given
//        Long orderId = 1L;
//        Long adminId = 999L;
//        Role role = Role.ADMIN;
//
//        given(orderRepository.findById(orderId)).willReturn(Optional.of(testOrder));
//        given(orderRepository.save(any(Order.class))).willReturn(testOrder);
//        // when
//        orderService.cancelOrder(orderId, adminId, role);
//
//        // then
//        verify(orderRepository, times(1)).findById(orderId);
//        verify(kafkaProducerService, times(1)).sendCancelRequest(any(PayCancelRequestEvent.class));
//    }

    @Test
    @DisplayName("주문 상세 조회 성공 테스트 - 관리자")
    void getOrderDetail_Success_Admin() {
        // given
        Long orderId = 1L;
        Long adminId = 999L;
        Role role = Role.ADMIN;

        UserBatchResponseDTO userResponse = UserBatchResponseDTO.builder()
                .id(1L)
                .memberId(1L)
                .owner("테스트사용자")
                .email("test@test.com")
                .build();

        PartDetailResponseDTO partResponse = PartDetailResponseDTO.builder()
                .id(101L)
                .name("에어필터")
                .categoryName("엔진부품")
                .price(50000)
                .cost(30000)
                .amount(100)
                .build();

        given(orderRepository.findById(orderId)).willReturn(Optional.of(testOrder));
        given(userService.getUsersByMemberIds(anyList())).willReturn(Map.of(1L, userResponse));
        given(inventoryService.getPartDetails(anyList())).willReturn(Map.of(101L, partResponse));

        // when
        OrderDetailResponseDTO response = orderService.getOrderDetail(orderId, adminId, role);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(1L);
        assertThat(response.getOrderNumber()).isEqualTo("SMO-1");
        assertThat(response.getMemberId()).isEqualTo(1L);

        verify(orderRepository, times(1)).findById(orderId);
        verify(userService, times(1)).getUsersByMemberIds(anyList());
        verify(inventoryService, times(1)).getPartDetails(anyList());
    }

    @Test
    @DisplayName("주문 검증 조회 성공 테스트")
    void getValidateOrder_Success() {
        // given
        Long orderId = 1L;
        Long memberId = 1L;

        given(orderRepository.findById(orderId)).willReturn(Optional.of(testOrder));

        // when
        OrderValidateDTO response = orderService.getValidateOrder(orderId, memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(1L);
        assertThat(response.getOrderNumber()).isEqualTo("SMO-1");

        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    @DisplayName("배송 등록 성공 테스트")
    void registerShipping_Success() {
        // given
        Order pendingShippingOrder = testOrder.toBuilder()
                .orderStatus(OrderStatus.PENDING_SHIPPING)
                .build();

        ShippingRegistrationRequestDTO requestDTO = ShippingRegistrationRequestDTO.builder()
                .orderNumber("SMO-1")
                .build();

        given(orderRepository.findByOrderNumber("SMO-1")).willReturn(Optional.of(pendingShippingOrder));
        given(orderRepository.save(any(Order.class))).willReturn(pendingShippingOrder);

        // when
        ShippingRegistrationResponseDTO response = orderService.registerShipping(requestDTO, Role.WAREHOUSE);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getOrderNumber()).isEqualTo("SMO-1");
        assertThat(response.getCarrier()).isEqualTo("현대글로비스");
        assertThat(response.getTrackingNumber()).isNotNull();
        assertThat(response.getTrackingNumber()).hasSize(13);

        verify(orderRepository, times(1)).findByOrderNumber("SMO-1");
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("주문 승인 상태 체크 성공 테스트")
    void checkOrderApprovalStatus_Success() {
        // given
        Long orderId = 1L;
        Long memberId = 1L;
        Role role = Role.USER;

        given(orderRepository.findById(orderId)).willReturn(Optional.of(testOrder));

        // when
        OrderApprovalStatusDTO response = orderService.checkOrderApprovalStatus(orderId, memberId, role);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(1L);
        assertThat(response.getOrderNumber()).isEqualTo("SMO-1");
        assertThat(response.getStatus()).isEqualTo(OrderStatus.ORDER_COMPLETED);

        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    @DisplayName("네비게이션용 부품 정보 조회 성공 테스트")
    void getPartsForNavigation_Success() {
        // given
        List<String> orderNumbers = List.of("SMO-1");

        PartDetailResponseDTO partDetail = PartDetailResponseDTO.builder()
                .id(101L)
                .name("에어필터")
                .location("A1-1")
                .build();

        given(orderRepository.findAllByOrderNumberIn(orderNumbers)).willReturn(List.of(testOrder));
        given(inventoryService.getPartDetails(anyList())).willReturn(Map.of(101L, partDetail));

        // when
        NavigationPartsResponseDTO response = orderService.getPartsForNavigation(orderNumbers);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getPartLocations()).hasSize(1);
        assertThat(response.getPartLocations().get(0).getPartId()).isEqualTo(101L);
        assertThat(response.getPartLocations().get(0).getLocation()).isEqualTo("A1-1");

        verify(orderRepository, times(1)).findAllByOrderNumberIn(orderNumbers);
        verify(inventoryService, times(1)).getPartDetails(anyList());
    }
}

