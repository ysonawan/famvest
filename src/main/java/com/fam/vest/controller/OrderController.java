package com.fam.vest.controller;

import com.fam.vest.dto.request.OrderRequest;
import com.fam.vest.dto.response.VirtualContractNotesDto;
import com.fam.vest.util.CommonUtil;
import com.fam.vest.util.UserDetailsUtil;
import com.zerodhatech.models.Order;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.fam.vest.dto.response.OrderDetails;
import com.fam.vest.service.OrderService;

@Slf4j
@RestController
@RequestMapping("/rest/v1/orders")
@CrossOrigin
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping()
    public ResponseEntity<Object> getOrders() {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Fetching orders for {}", userDetails.getUsername());
        List<OrderDetails> orders = orderService.getOrders(userDetails);
        return CommonUtil.success(orders);
    }

    @PostMapping()
    public ResponseEntity<Object> placeOrder(@RequestBody OrderRequest orderRequest,
                                             @RequestParam(required = true) String variety) {

        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Creating order for request: {} by: {}", orderRequest, userDetails.getUsername());
        Order orderResponse = orderService.placeOrder(userDetails, orderRequest, variety);
        return CommonUtil.success(orderResponse);
    }

    @PutMapping("/{tradingAccountId}/{orderId}")
    public ResponseEntity<Object> modifyOrder(@PathVariable String orderId,
                                              @PathVariable String tradingAccountId,
                                              @RequestBody OrderRequest orderRequest,
                                              @RequestParam(required = true) String variety) {

        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Modify order for order id: {} trading account: {} by: {}", orderId, tradingAccountId, userDetails.getUsername());
        Order orderResponse = orderService.modifyOrder(orderId, userDetails, orderRequest, variety);
        return CommonUtil.success(orderResponse);
    }

    @DeleteMapping("/{tradingAccountId}/{orderId}")
    public ResponseEntity<Object> cancelOrder(@PathVariable String orderId,
                                              @PathVariable String tradingAccountId,
                                              @RequestParam(required = true) String variety) {

        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Cancel order for order id: {} trading account: {} by: {}", orderId, tradingAccountId, userDetails.getUsername());
        Order orderResponse = orderService.cancelOrder(userDetails, orderId, tradingAccountId, variety);
        return CommonUtil.success(orderResponse);
    }

    @GetMapping("/charges")
    public ResponseEntity<Object> getVirtualContractNotes() {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Fetching virtual contract notes for completed orders by: {}", userDetails.getUsername());
        List<VirtualContractNotesDto> virtualContractNotesDtoList = orderService.getVirtualContractNotes(userDetails);
        return CommonUtil.success(virtualContractNotesDtoList);
    }

}
