package com.fam.vest.service;

import com.fam.vest.dto.response.OrderDetails;
import com.fam.vest.dto.request.OrderRequest;
import com.fam.vest.dto.response.VirtualContractNotesDto;
import com.zerodhatech.models.Order;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public interface OrderService {

    List<OrderDetails> getAllOrders();

    List<OrderDetails> getOrders(UserDetails userDetails);

    Order placeOrder(UserDetails userDetails, OrderRequest orderRequest, String variety);

    Order modifyOrder(String orderId, UserDetails userDetails, OrderRequest orderRequest, String variety);

    Order cancelOrder(UserDetails userDetails, String orderId, String tradingAccountId, String variety);

    List<VirtualContractNotesDto> getVirtualContractNotes(UserDetails userDetails);
}
