package com.zextras.carbonio.chats.it.utils;

import java.time.Duration;

public class TimeUtils {

  public static String durationToString(Duration duration) {
    long l = duration.toMillis();
    long h = l / 3600000L;
    int m = (int) (l % 3600000L / 60000L);
    int s = (int) (l % 60000L / 1000L);
    int ms = (int) (l % 1000L);
    StringBuilder sb = new StringBuilder();
    sb.append(h > 0 ? h + "h " : "");
    sb.append(m > 0 || h > 0 ? m + "m " : "");
    sb.append(s > 0 || h + m > 0 ? s + "s " : "");
    sb.append(ms > 0 || h + m + s > 0 ? ms + "ms" : "");
    return sb.toString();
  }

}
