package com.fam.vest.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class IpoApplication {

    private String id;
    private String category;
    private String symbol;

    @JsonProperty("instrument_id")
    private String instrumentId;

    @JsonProperty("end_date")
    private String endDate;

    @JsonProperty("user_id")
    private String userId;

    private String exchange;
    private String status;

    @JsonProperty("upi_id")
    private String upiId;

    @JsonProperty("investor_type")
    private String investorType;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("allotment_rate")
    private int allotmentRate;

    @JsonProperty("allotment_quantity")
    private int allotmentQuantity;

    private List<IpoBid> bids;

    @JsonProperty("mod_counter")
    private int modCounter;

    @JsonProperty("dp_status")
    private String dpStatus;

    @JsonProperty("payment_status")
    private String paymentStatus;

    @JsonProperty("payment_remark")
    private String paymentRemark;

    @JsonProperty("amount_blocked")
    private int amountBlocked;

    @JsonProperty("app_id")
    private String appId;

    private String remark;

    @JsonProperty("is_pre_start")
    private boolean preStart;
}
