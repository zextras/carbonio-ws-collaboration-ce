package com.zextras.carbonio.chats.meeting.config;

import com.google.inject.AbstractModule;
import com.zextras.carbonio.chats.meeting.api.MeetingsApi;
import com.zextras.carbonio.chats.meeting.api.MeetingsApiService;
import com.zextras.carbonio.chats.meeting.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.meeting.infrastructure.videoserver.impl.VideoServerServiceMock;
import com.zextras.carbonio.chats.meeting.repository.MeetingRepository;
import com.zextras.carbonio.chats.meeting.repository.impl.MeetingRepositoryImpl;
import com.zextras.carbonio.chats.meeting.service.MeetingService;
import com.zextras.carbonio.chats.meeting.service.impl.MeetingServiceImpl;
import com.zextras.carbonio.chats.meeting.web.api.MeetingsApiServiceImpl;

public class MeetingModule extends AbstractModule {

  @Override
  protected void configure() {
    super.configure();

    bind(MeetingsApi.class);
    bind(MeetingsApiService.class).to(MeetingsApiServiceImpl.class);
    bind(MeetingService.class).to(MeetingServiceImpl.class);
    bind(MeetingRepository.class).to(MeetingRepositoryImpl.class);

    bind(VideoServerService.class).to(VideoServerServiceMock.class);
  }
}
