package com.fam.vest.service;

import com.fam.vest.pojo.email.ResendEmailPayload;

public interface EmailService {

    void sendEmail(ResendEmailPayload resendEmailPayload);
}
