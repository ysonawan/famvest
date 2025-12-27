package com.fam.vest.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class IpoResponse {

    private String status;
    private List<IpoData> data;

}
