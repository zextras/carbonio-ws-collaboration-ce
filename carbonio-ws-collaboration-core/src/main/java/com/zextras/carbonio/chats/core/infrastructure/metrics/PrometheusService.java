// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.metrics;

import com.google.inject.Singleton;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import java.time.Duration;

@Singleton
public class PrometheusService {

  private final PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
  private final Counter chatCreation = prometheusRegistry.counter("wsc.chat","service","wsc","type","oneOnOne");
  private final Counter groupChatCreation = prometheusRegistry.counter("wsc.chat","service","wsc","type","group");
  private final Counter oneOnOneMeetingStart = prometheusRegistry.counter("wsc.meeting","service","wsc","type","oneOnOne");
  private final Counter groupMeetingStart       = prometheusRegistry.counter("wsc.meeting","service","wsc","type","group");
  private final Timer   oneOnOneMeetingDuration = prometheusRegistry.timer("wsc.meeting.duration","service","wsc","type","oneOnOne");
  private final Timer   groupMeetingDuration    = prometheusRegistry.timer("wsc.meeting.duration","service","wsc","type","group");

  public PrometheusMeterRegistry getRegistry(){
    return prometheusRegistry;
  }

  public void incrementChatCreation(){
    chatCreation.increment();
  }

  public void incrementGroupChatCreation(){
    groupChatCreation.increment();
  }

  public void incrementOneOnOneMeetingStart(){
    oneOnOneMeetingStart.increment();
  }

  public void incrementGroupMeetingStart(){
    groupMeetingStart.increment();
  }

  public void recordOneOnOneMeetingDuration(Duration duration){
    oneOnOneMeetingDuration.record(duration);
  }

  public void recordGroupMeetingDuration(Duration duration){
    groupMeetingDuration.record(duration);
  }
}