package com.nfyc.lcnotificationservice.handlers;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;
import java.util.logging.Level;

@RequiredArgsConstructor
@Component
public class TimerTriggerRevisionEmailAlertHandler {

  private final Supplier<String> triggerRevisionLcEmailAlerts;

  @FunctionName("triggerRevisionLcEmailAlerts")
  public void execute(
      @TimerTrigger(name = "timerInfo", schedule = "0 30 10 * * *") String timerInfo,
      final ExecutionContext context
  ) {
    try {
      String response = triggerRevisionLcEmailAlerts.get();
      context.getLogger().info("Executed the triggerRevisionLcEmailAlerts function: " + response);
    } catch (Exception e) {
      context.getLogger().log(Level.SEVERE, "Error occurred while executing triggerRevisionLcEmailAlerts" + e.getMessage());
      throw e;
    }
  }
}
