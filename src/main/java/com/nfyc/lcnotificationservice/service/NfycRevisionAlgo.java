package com.nfyc.lcnotificationservice.service;

import com.nfyc.lcnotificationservice.domain.LcAttemptedQuestion;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;

@Service
public class NfycRevisionAlgo {

  private static final int HARD_FF = 12;
  private static final int MEDIUM_FF  = 10;
  private static final int EASY_FF = 7;
  public Date getNextRevisionDate(Date lrd, int priority, int revisionCount) {
      int factor = Math.min(31, (revisionCount * getFrequencyFactor(priority)) / priority);
      Calendar cal = Calendar.getInstance();
      cal.setTime(lrd);
      cal.add(Calendar.DATE, factor);
      return cal.getTime();
  }

  private int getFrequencyFactor(int priority) {
    if (priority == 1) {
      return EASY_FF;
    } else if (priority == 2) {
      return MEDIUM_FF;
    } else  {
      return HARD_FF;
    }
  }

  public Comparator<LcAttemptedQuestion> getNfycRevisionComparator() {
    return Comparator.comparing(LcAttemptedQuestion::getNextRevisionDate)
        .thenComparing(LcAttemptedQuestion::getPriority, (a, b) -> b - a)
        .thenComparing(LcAttemptedQuestion::getLastRevisedDate)
        .thenComparingInt(LcAttemptedQuestion::getRevisionCount);
  }

  public boolean isNextRevisionDayLessThanToday(Date date1) {
    return date1.compareTo(new Date()) <= 0;
  }

  public boolean isPreviousDay(Date date) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());
    calendar.add(Calendar.DAY_OF_YEAR, -1);

    Calendar submission = Calendar.getInstance();
    submission.setTime(date);

    return calendar.get(Calendar.DAY_OF_YEAR) == submission.get(Calendar.DAY_OF_YEAR)
        && calendar.get(Calendar.MONTH) == submission.get(Calendar.MONTH)
        && calendar.get(Calendar.YEAR) == submission.get(Calendar.YEAR);

  }
}
