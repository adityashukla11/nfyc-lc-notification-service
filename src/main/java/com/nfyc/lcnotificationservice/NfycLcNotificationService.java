package com.nfyc.lcnotificationservice;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nfyc.lcnotificationservice.domain.NfycLcUser;
import com.nfyc.lcnotificationservice.functions.NfycLcFunctions;
import com.nfyc.lcnotificationservice.service.NfycLeetcodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;

import java.util.function.Function;
import java.util.logging.Logger;

@SpringBootApplication
public class NfycLcNotificationService {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(NfycLcFunctions.class, args);
    }
}
