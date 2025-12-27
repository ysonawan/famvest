package com.fam.vest.pojo.email;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResendEmailPayload {

    private String from;
    private String[] to;
    private String[] cc;
    private String[] bcc;
    private String subject;
    private String html;
    private String text;
    private String scheduledAt;
}
