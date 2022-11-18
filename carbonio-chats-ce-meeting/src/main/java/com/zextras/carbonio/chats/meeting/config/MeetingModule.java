package com.zextras.carbonio.chats.meeting.config;

import com.google.inject.AbstractModule;
import com.zextras.carbonio.chats.meeting.api.TestApi;
import com.zextras.carbonio.chats.meeting.api.TestApiService;
import com.zextras.carbonio.chats.meeting.service.TestService;
import com.zextras.carbonio.chats.meeting.service.impl.TestServiceImpl;
import com.zextras.carbonio.chats.meeting.web.api.TestApiServiceImpl;

public class MeetingModule extends AbstractModule {

  @Override
  protected void configure() {
    super.configure();

    bind(TestApi.class);
    bind(TestApiService.class).to(TestApiServiceImpl.class);
    bind(TestService.class).to(TestServiceImpl.class);

  }
}
