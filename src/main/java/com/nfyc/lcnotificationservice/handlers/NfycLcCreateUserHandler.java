package com.nfyc.lcnotificationservice.handlers;

import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.nfyc.lcnotificationservice.domain.NfycLcResponse;
import com.nfyc.lcnotificationservice.domain.NfycLcUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.microsoft.azure.functions.ExecutionContext;

import java.util.Optional;
import java.util.function.Function;

@RequiredArgsConstructor
@Service
public class NfycLcCreateUserHandler {

    private final Function<NfycLcUser, NfycLcResponse> registerUserForEmailAlert;

    @FunctionName("createLcUser")
    public NfycLcResponse execute(@HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<NfycLcUser>> request,
                                  ExecutionContext context) {
        return registerUserForEmailAlert.apply(request.getBody().orElseThrow(() ->
                new RuntimeException("User Doesn't exisit")));
    }
}
