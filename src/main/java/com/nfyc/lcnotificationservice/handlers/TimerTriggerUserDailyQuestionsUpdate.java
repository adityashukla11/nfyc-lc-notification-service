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
public class TimerTriggerUserDailyQuestionsUpdate {

  private final Supplier<String> triggerUserACSubmissionUpdates;

  @FunctionName("triggerUserACSubmissionUpdates")
  public void execute(
      @TimerTrigger(name = "timerInfo", schedule = "0 30 0 * * *") String timerInfo,
      final ExecutionContext context
  ) {
    try {
      String response = triggerUserACSubmissionUpdates.get();
      context.getLogger().info("Executed the triggerUserACSubmissionUpdates function: " + response);
    } catch (Exception e) {
      context.getLogger().log(Level.SEVERE, "Error occurred while executing triggerUserACSubmissionUpdates" + e.getMessage());
      throw e;
    }
  }
}
