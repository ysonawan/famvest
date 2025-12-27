package com.fam.vest.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class IpoData {

    private String id;
    private String symbol;
    private boolean active;
    private String status;
    private String registrar;

    @JsonProperty("sub_type")
    private String subType;

    @JsonProperty("max_bid_count")
    private int maxBidCount;

    private String name;
    private String isin;

    @JsonProperty("issue_type")
    private String issueType;

    @JsonProperty("face_value")
    private int faceValue;

    @JsonProperty("issue_size")
    private long issueSize;

    @JsonProperty("tick_size")
    private int tickSize;

    @JsonProperty("lot_size")
    private int lotSize;

    @JsonProperty("min_qty")
    private int minQty;

    @JsonProperty("max_qty")
    private int maxQty;

    @JsonProperty("start_at")
    private String startAt;   // If you want LocalDateTime -> replace with LocalDateTime

    @JsonProperty("end_at")
    private String endAt;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("allotment_finalisation_date")
    private String allotmentFinalisationDate;

    @JsonProperty("refund_initiation_date")
    private String refundInitiationDate;

    @JsonProperty("demat_transfer_date")
    private String dematTransferDate;

    @JsonProperty("listing_date")
    private String listingDate;

    @JsonProperty("mandate_end_date")
    private String mandateEndDate;

    @JsonProperty("rhp_link")
    private String rhpLink;

    private String info;

    @JsonProperty("bond_series")
    private List<Object> bondSeries;

    @JsonProperty("investor_types")
    private List<InvestorType> investorTypes;

    private List<String> exchanges;

    private Object orders;
}
