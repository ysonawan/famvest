package com.fam.vest.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class IpoApplicationResponse {
    private String status;
    private List<IpoApplication> data;
}
