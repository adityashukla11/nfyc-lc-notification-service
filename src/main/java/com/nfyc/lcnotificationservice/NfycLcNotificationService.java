package com.nfyc.lcnotificationservice;


import com.nfyc.lcnotificationservice.functions.NfycLcFunctions;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class NfycLcNotificationService {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(NfycLcFunctions.class, args);
    }
}
