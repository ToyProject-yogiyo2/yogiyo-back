package toy.yogiyo.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import toy.yogiyo.common.login.LoginUser;
import toy.yogiyo.core.member.domain.Member;
import toy.yogiyo.core.order.dto.OrderCreateRequest;
import toy.yogiyo.core.order.dto.OrderDetailResponse;
import toy.yogiyo.core.order.dto.OrderHistoryResponse;
import toy.yogiyo.core.order.service.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    public void createOrder(@LoginUser Member member, @RequestBody OrderCreateRequest orderCreateRequest){
        orderService.createOrder(member, orderCreateRequest);
    }

    @GetMapping("/scroll")
    public OrderHistoryResponse scrollOrderHistories(@LoginUser Member member, @RequestParam(defaultValue = "-1") Long lastId){
        return orderService.getOrderHistory(member, lastId);
    }

    @GetMapping("/details")
    public OrderDetailResponse getOrderDetails(@LoginUser Member member, @RequestParam Long orderId){
        return orderService.getOrderDetail(member, orderId);
    }
}
