package com.nfyc.lcnotificationservice.handlers;

import java.time.*;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.microsoft.azure.functions.ExecutionContext;

import java.util.function.Supplier;
import java.util.logging.Level;

@RequiredArgsConstructor
@Component
public class TimerTriggerLcEmailAlertHandler {

    private final Supplier<String> triggerLcEmailAlerts;
    @FunctionName("triggerLcEmailAlerts")
    public void execute(
        @TimerTrigger(name = "timerInfo", schedule = "0 0 20,22 * *") String timerInfo,
        final ExecutionContext context
    ) {

        try {
            String response = triggerLcEmailAlerts.get();
            context.getLogger().info("Executed the email alert trigger function: " + response);
        } catch (Exception e) {
            context.getLogger().log(Level.SEVERE, "Error occurred while executing email " + e.getMessage());
            throw e;
        }
    }
}
