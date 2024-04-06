package com.nfyc.lcnotificationservice.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@ToString
@Builder
@Getter
@Setter
public class LcAttemptedQuestion{
  private String rowKey;
  private Date lastRevisedDate;
  private Date lastSolvedDate;
  private Date nextRevisionDate;
  private int priority;
  private int revisionCount;
  private String questionTitle;
  private String questionTitleSlug;
}
