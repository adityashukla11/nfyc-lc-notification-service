package com.nfyc.lcnotificationservice.handlers;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.nfyc.lcnotificationservice.domain.NfycLcResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Supplier;

@RequiredArgsConstructor
@Component
public class LcDailyChallengeHandler  {

    private final Supplier<NfycLcResponse> getLcDailyChallenge;

    @FunctionName("getLcDailyChallenge")
    public NfycLcResponse execute(@HttpTrigger(name = "req", methods = {HttpMethod.GET}, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
                                        ExecutionContext context) {
        return getLcDailyChallenge.get();
    }
}
