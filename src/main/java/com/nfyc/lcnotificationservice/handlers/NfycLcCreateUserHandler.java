package com.nfyc.lcnotificationservice.handlers;

import com.azure.core.http.HttpResponse;
import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.data.tables.models.TableEntity;
import com.azure.data.tables.models.TableServiceException;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.nfyc.lcnotificationservice.domain.NfycLcResponse;
import com.nfyc.lcnotificationservice.domain.NfycLcUser;
import com.nfyc.lcnotificationservice.utils.NfycLcEcxeption;
import com.nfyc.lcnotificationservice.utils.NfycLcError;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.microsoft.azure.functions.ExecutionContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@RequiredArgsConstructor
@Service
public class NfycLcCreateUserHandler {

    private final Function<NfycLcUser, NfycLcResponse> registerUserForEmailAlert;

    @FunctionName("createLcUser")
    public HttpResponseMessage execute(@HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<NfycLcUser>> request,
                                       ExecutionContext context) {
        try {
            NfycLcResponse nfycLcResponse = registerUserForEmailAlert.apply(request.getBody().orElseThrow(() ->
                new NfycLcEcxeption(NfycLcError.ERROR_NO_API_PAYLOAD)));
            return request.createResponseBuilder(HttpStatus.OK).body(nfycLcResponse.getData()
                .get("message").toString()).build();
        } catch (NfycLcEcxeption e) {
            Map<String, String> error = new HashMap<>();
            error.put("code", e.getErrorCode().toString());
            error.put("message", e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body(error).build();
        }
    }
}
