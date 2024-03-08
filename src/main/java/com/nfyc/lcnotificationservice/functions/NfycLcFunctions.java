package com.nfyc.lcnotificationservice.functions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nfyc.lcnotificationservice.domain.NfycLcResponse;
import com.nfyc.lcnotificationservice.domain.NfycLcUser;
import com.nfyc.lcnotificationservice.service.NfycLeetcodeService;
import lombok.extern.slf4j.Slf4j;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@Service
public class NfycLcFunctions {

    @Autowired
    private NfycLeetcodeService nfycLeetcodeService;


    @Bean(name = "registerUserForEmailAlert")
    public Function<NfycLcUser, NfycLcResponse> registerUserForEmailAlert() {
        return newUser -> nfycLeetcodeService.saveLcUser(newUser);
    }

    @Bean("getLcDailyChallenge")
    public Supplier<NfycLcResponse> getLcDailyChallenge() {
        return () -> nfycLeetcodeService.getDailyChallenge();
    }

    @Bean("getAllNfycUser")
    public Supplier<List<NfycLcUser>> getAllNfycUser() {
        return () -> nfycLeetcodeService.getAllNfycUser();
    }
}
