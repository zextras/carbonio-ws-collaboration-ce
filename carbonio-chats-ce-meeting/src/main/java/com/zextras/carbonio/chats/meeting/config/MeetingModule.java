package com.zextras.carbonio.chats.meeting.config;

import com.google.inject.AbstractModule;
import com.zextras.carbonio.chats.meeting.api.MeetingsApi;
import com.zextras.carbonio.chats.meeting.api.MeetingsApiService;
import com.zextras.carbonio.chats.meeting.api.RoomsApi;
import com.zextras.carbonio.chats.meeting.api.RoomsApiService;
import com.zextras.carbonio.chats.meeting.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.meeting.infrastructure.videoserver.impl.VideoServerServiceMock;
import com.zextras.carbonio.chats.meeting.mapper.MeetingMapper;
import com.zextras.carbonio.chats.meeting.mapper.ParticipantMapper;
import com.zextras.carbonio.chats.meeting.mapper.impl.MeetingMapperImpl;
import com.zextras.carbonio.chats.meeting.mapper.impl.ParticipantMapperImpl;
import com.zextras.carbonio.chats.meeting.repository.MeetingRepository;
import com.zextras.carbonio.chats.meeting.repository.impl.MeetingRepositoryImpl;
import com.zextras.carbonio.chats.meeting.service.MeetingService;
import com.zextras.carbonio.chats.meeting.service.impl.MeetingServiceImpl;
import com.zextras.carbonio.chats.meeting.web.api.MeetingsApiServiceImpl;
import com.zextras.carbonio.chats.meeting.web.api.RoomsApiServiceImpl;

public class MeetingModule extends AbstractModule {

  @Override
  protected void configure() {
    super.configure();

    bind(RoomsApi.class);
    bind(RoomsApiService.class).to(RoomsApiServiceImpl.class);

    bind(MeetingsApi.class);
    bind(MeetingsApiService.class).to(MeetingsApiServiceImpl.class);
    bind(MeetingService.class).to(MeetingServiceImpl.class);
    bind(MeetingRepository.class).to(MeetingRepositoryImpl.class);
    bind(MeetingMapper.class).to(MeetingMapperImpl.class);
    bind(ParticipantMapper.class).to(ParticipantMapperImpl.class);

    bind(VideoServerService.class).to(VideoServerServiceMock.class);
  }
}
