package com.zextras.carbonio.chats.meeting.it.config;

import com.google.inject.AbstractModule;
import com.zextras.carbonio.chats.meeting.it.utils.MeetingTestUtils;

public class MeetingTestModule extends AbstractModule {

  @Override
  protected void configure() {
    super.configure();
    bind(MeetingTestUtils.class);
  }

}

