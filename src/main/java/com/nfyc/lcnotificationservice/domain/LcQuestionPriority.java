package com.nfyc.lcnotificationservice.domain;

public enum LcQuestionPriority {
  Hard (3),
  Medium (2),
  Easy (1);

  private final int priority;
  LcQuestionPriority(int priority) {
    this.priority = priority;
  }

  public int getPriority() {
    return priority;
  }
}
