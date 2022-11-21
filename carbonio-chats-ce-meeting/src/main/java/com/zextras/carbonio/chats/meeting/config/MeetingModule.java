package com.zextras.carbonio.chats.meeting.config;

import com.google.inject.AbstractModule;
import com.zextras.carbonio.chats.meeting.api.MeetingsApi;
import com.zextras.carbonio.chats.meeting.api.MeetingsApiService;
import com.zextras.carbonio.chats.meeting.web.api.MeetingsApiServiceImpl;

public class MeetingModule extends AbstractModule {

  @Override
  protected void configure() {
    super.configure();

    bind(MeetingsApi.class);
    bind(MeetingsApiService.class).to(MeetingsApiServiceImpl.class);

  }
}
