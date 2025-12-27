package com.fam.vest.service.implementation;

import com.fam.vest.config.KiteConnector;
import com.fam.vest.dto.response.ContractNoteDto;
import com.fam.vest.dto.response.VirtualContractNotesDto;
import com.fam.vest.entity.Instrument;
import com.fam.vest.entity.TradingAccount;
import com.fam.vest.exception.InternalException;
import com.fam.vest.exception.RequestTokenMissingException;
import com.fam.vest.exception.ResourceNotFoundException;
import com.fam.vest.dto.response.OrderDetails;
import com.fam.vest.dto.request.OrderRequest;
import com.fam.vest.service.InstrumentService;
import com.fam.vest.service.OrderService;
import com.fam.vest.service.QuoteService;
import com.fam.vest.service.TradingAccountService;
import com.fam.vest.util.CommonUtil;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.kiteconnect.utils.Constants;
import com.zerodhatech.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class IOrderService implements OrderService {

    private final TradingAccountService tradingAccountService;
    private final InstrumentService instrumentService;
    private final KiteConnector kiteConnector;
    private QuoteService quoteService;

    @Autowired
    public IOrderService(TradingAccountService tradingAccountService,
                         InstrumentService instrumentService,
                         KiteConnector kiteConnector,
                         QuoteService quoteService) {
        this.tradingAccountService = tradingAccountService;
        this.instrumentService = instrumentService;
        this.kiteConnector = kiteConnector;
        this.quoteService = quoteService;
    }

    @Override
    public List<OrderDetails> getAllOrders() {
        return this.getOrders(null);
    }

    @Override
    public List<OrderDetails> getOrders(UserDetails userDetails) {
        List<TradingAccount> tradingAccounts = null;
        if(null == userDetails) {
            tradingAccounts = tradingAccountService.getAllTradingAccounts();
        } else {
            tradingAccounts = tradingAccountService.getTradingAccounts(userDetails, true);
        }
        List<OrderDetails> orderDetails = new ArrayList<>();
        AtomicLong sequenceNumber = new AtomicLong(1);
        tradingAccounts.forEach(tradingAccount -> {
            try {
                KiteConnect kiteConnect = kiteConnector.getKiteConnect(tradingAccount);
                this.getOrders(kiteConnect, tradingAccount.getId(), orderDetails, sequenceNumber);
            } catch (RequestTokenMissingException e) {
                log.warn("RequestToken missing for trading user: {}. Skipped", tradingAccount.getUserId());
            } catch (KiteException | IOException e) {
                String errorMessage = CommonUtil.getExceptionMessage(e);
                log.error("Error while getting orders for trading user: {}. Error: {}", tradingAccount.getUserId(), errorMessage, e);
                throw new InternalException(errorMessage);
            }
        });
        return orderDetails;
    }

    private void getOrders(KiteConnect kiteConnect, Long tradingAccountId, List<OrderDetails> orderDetails, AtomicLong sequenceNumber) throws KiteException, IOException {
        List<Order> orders = kiteConnect.getOrders();
        orders.forEach(order -> {
            OrderDetails orderDetail = this.convertToOrderDetails(order, tradingAccountId, kiteConnect.getUserId());
            orderDetail.setSequenceNumber(sequenceNumber.getAndIncrement());
            orderDetails.add(orderDetail);
        });
        //sort using order timestamp in descending order
        orderDetails.sort((o1, o2) -> o2.getOrder().orderTimestamp.compareTo(o1.getOrder().orderTimestamp));
    }

    private OrderDetails convertToOrderDetails(Order order, Long tradingAccountId, String userId) {
        OrderDetails orderDetails = new OrderDetails();
        orderDetails.setTradingAccountId(tradingAccountId);
        orderDetails.setUserId(userId);
        orderDetails.setOrder(order);
        String displayName = order.tradingSymbol;
        Instrument instrument = null;
        try {
            instrument = instrumentService.getByTradingSymbolAndExchange(order.tradingSymbol, order.exchange);
            if(null != instrument) {
                displayName = instrument.getDisplayName();
                orderDetails.setInstrumentToken(instrument.getInstrumentToken());
                String quoteInstrument = instrument.getExchange() + ":" + instrument.getTradingSymbol();
                Map<String, Quote> quote = quoteService.getQuote(quoteInstrument);
                if (quote.containsKey(quoteInstrument)) {
                    Quote instrumentQuote = quote.get(quoteInstrument);
                    orderDetails.setLastPrice(instrumentQuote.lastPrice);
                    orderDetails.setChange(instrumentQuote.change);
                } else {
                    log.warn("Quote not found for instrument: {}", quoteInstrument);
                    orderDetails.setLastPrice(instrument.getLastPrice());
                    orderDetails.setChange(0.0);
                }
            }
        } catch (ResourceNotFoundException e) {
            log.error("Instrument not found for symbol: {} exchange: {}", order.tradingSymbol, order.exchange);
        }
        orderDetails.setDisplayName(displayName);
        return orderDetails;
    }

    @Override
    public Order modifyOrder(String orderId, UserDetails userDetails, OrderRequest orderRequest, String variety) {
        Order orderResponse = null;
        try {
            TradingAccount tradingAccount = tradingAccountService.getTradingAccount(userDetails, orderRequest.getTradingAccountId());
            KiteConnect kiteConnect = kiteConnector.getKiteConnect(tradingAccount);
            orderResponse = this.modifyRegularOrder(kiteConnect, orderId, orderRequest, variety);
        } catch (RequestTokenMissingException e) {
            String errorMessage = "Request token missing for trading user: "+orderRequest.getTradingAccountId()+" .Order modification skipped";
            log.error(errorMessage);
            throw new InternalException(errorMessage);
        } catch (KiteException | IOException e) {
            String errorMessage = CommonUtil.getExceptionMessage(e);
            log.error("Error while modifying order for userId: {}. Error: {}", orderRequest.getTradingAccountId(), errorMessage, e);
            throw new InternalException(errorMessage);
        }
        return orderResponse;
    }

    @Override
    public Order placeOrder(UserDetails userDetails, OrderRequest orderRequest, String variety) {
        Order orderResponse = null;
        try {
            TradingAccount tradingAccount = tradingAccountService.getTradingAccount(userDetails, orderRequest.getTradingAccountId());
            KiteConnect kiteConnect = kiteConnector.getKiteConnect(tradingAccount);
            orderResponse = this.placeRegularOrder(kiteConnect, orderRequest, variety);
        } catch (RequestTokenMissingException e) {
            String errorMessage = "Request token missing for trading user: "+orderRequest.getTradingAccountId()+" .Order placement skipped";
            log.error(errorMessage);
            throw new InternalException(errorMessage);
        } catch (KiteException | IOException e) {
            String errorMessage = CommonUtil.getExceptionMessage(e);
            log.error("Error while creating order for userId: {}. Error: {}", orderRequest.getTradingAccountId(), errorMessage, e);
            throw new InternalException(errorMessage);
        }
        return orderResponse;
    }

    private Order modifyRegularOrder(KiteConnect kiteConnect, String orderId, OrderRequest orderRequest, String variety) throws IOException, KiteException {
        OrderParams orderParams = orderRequest.getOrderParams();
        return kiteConnect.modifyOrder(orderId, orderParams, variety);
    }

    private Order placeRegularOrder(KiteConnect kiteConnect, OrderRequest orderRequest, String variety) throws IOException, KiteException {
        OrderParams orderParams = orderRequest.getOrderParams();
        return kiteConnect.placeOrder(orderParams, variety);
    }

    @Override
    public Order cancelOrder(UserDetails userDetails, String orderId, String tradingAccountId, String variety) {
        Order orderResponse = null;
        try {
            TradingAccount tradingAccount = tradingAccountService.getTradingAccount(userDetails, tradingAccountId);
            KiteConnect kiteConnect = kiteConnector.getKiteConnect(tradingAccount);
            orderResponse = this.canceRegularlOrder(kiteConnect, orderId, variety);
        } catch (RequestTokenMissingException e) {
            String errorMessage = "Request token missing for trading user: "+tradingAccountId+" .Order cancellation skipped";
            log.error(errorMessage);
            throw new InternalException(errorMessage);
        } catch (KiteException | IOException e) {
            String errorMessage = CommonUtil.getExceptionMessage(e);
            log.error("Error while cancelling order for userId: {}. Error: {}", tradingAccountId, errorMessage, e);
            throw new InternalException(errorMessage);
        }
        return orderResponse;
    }

    private Order canceRegularlOrder(KiteConnect kiteConnect, String orderId, String variety) throws KiteException, IOException {
        return kiteConnect.cancelOrder(orderId, variety);
    }

    @Override
    public List<VirtualContractNotesDto> getVirtualContractNotes(UserDetails userDetails) {
        List<VirtualContractNotesDto> virtualContractNotesDtoList = new ArrayList<>();
        List<OrderDetails> orderDetails =  this.getOrders(userDetails);
        List<TradingAccount> tradingAccounts = tradingAccountService.getTradingAccounts(userDetails, true);
        tradingAccounts.forEach(tradingAccount -> {
            try {
                List<OrderDetails> userOrders = orderDetails.stream()
                        .filter(orderDetail -> orderDetail.getTradingAccountId().equals(tradingAccount.getId()) &&
                                orderDetail.getOrder().status.equalsIgnoreCase(Constants.ORDER_COMPLETE)).toList();
                List<ContractNoteParams> params = this.getContractNoteParams(userOrders, tradingAccount);
                if(!params.isEmpty()) {
                    KiteConnect kiteConnect = kiteConnector.getKiteConnect(tradingAccount);
                    VirtualContractNotesDto virtualContractNotesDto = new VirtualContractNotesDto();
                    List<ContractNote> contractNotes = kiteConnect.getVirtualContractNote(params);
                    virtualContractNotesDto.setTradingAccountId(tradingAccount.getId());
                    virtualContractNotesDto.setUserId(tradingAccount.getUserId());
                    List<ContractNoteDto> contractNotesDto = new ArrayList<>();
                    contractNotes.forEach(contractNote -> {
                        ContractNoteDto contractNoteDto = new ContractNoteDto();
                        contractNoteDto.setDisplayName(userOrders.stream().filter(order ->
                                        order.getOrder().tradingSymbol.equals(contractNote.tradingSymbol))
                                .findFirst()
                                .map(OrderDetails::getDisplayName)
                                .orElse(contractNote.tradingSymbol));
                        contractNoteDto.setContractNote(contractNote);
                        contractNotesDto.add(contractNoteDto);
                    });
                    virtualContractNotesDto.setContractNoteDtos(contractNotesDto);
                    virtualContractNotesDtoList.add(virtualContractNotesDto);
                }
            } catch (RequestTokenMissingException e) {
                log.warn("RequestToken missing for trading user: {}. Skipped", tradingAccount.getUserId());
            } catch (KiteException | IOException e) {
                String errorMessage = CommonUtil.getExceptionMessage(e);
                log.error("Error while getting virtual contract notes for trading user: {}. Error: {}", tradingAccount.getUserId(), errorMessage, e);
                throw new InternalException(errorMessage);
            }
        });
        return virtualContractNotesDtoList ;
    }

    private List<ContractNoteParams> getContractNoteParams(List<OrderDetails> orderDetails, TradingAccount tradingAccount) {
        List<ContractNoteParams> params = new ArrayList<>();
        orderDetails.forEach(orderDetail -> {
            ContractNoteParams contractNoteParams = new ContractNoteParams();
            contractNoteParams.orderID = orderDetail.getOrder().orderId;
            contractNoteParams.tradingSymbol = orderDetail.getOrder().tradingSymbol;
            contractNoteParams.exchange = orderDetail.getOrder().exchange;
            contractNoteParams.transactionType = orderDetail.getOrder().transactionType;
            contractNoteParams.variety = orderDetail.getOrder().orderVariety;
            contractNoteParams.product = orderDetail.getOrder().product;
            contractNoteParams.orderType = orderDetail.getOrder().orderType;
            contractNoteParams.quantity = Integer.parseInt(orderDetail.getOrder().quantity);
            contractNoteParams.averagePrice = Double.parseDouble(orderDetail.getOrder().averagePrice);
            params.add(contractNoteParams);
        });
        return params;
    }

}
