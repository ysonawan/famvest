package com.fam.vest.service;

import com.fam.vest.dto.request.IpoBidRequest;
import com.fam.vest.dto.response.*;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

public interface IpoService {

    List<IpoData> getIpos(Optional<String> status);

    List<IpoApplication> getIpoApplications(Optional<String> tradingAccountId, UserDetails userDetails);

    VpaData getVPA(String tradingAccountId, UserDetails userDetails);

    GeneralKiteData submitIpoApplication(String tradingAccountId, UserDetails userDetails, IpoBidRequest ipoBidRequest);

    GeneralKiteData cancelIpoApplication(String tradingAccountId, UserDetails userDetails, String applicationId);

    void refreshIposFromKiteInternalApi();

    void retrieveAndSaveIpos();

    void notifyOpenIpos();
}
