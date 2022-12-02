package com.zextras.carbonio.chats.meeting.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.meeting.api.MeetingsApi;
import com.zextras.carbonio.chats.meeting.api.MeetingsApiService;
import com.zextras.carbonio.chats.meeting.api.RoomsApi;
import com.zextras.carbonio.chats.meeting.api.RoomsApiService;
import com.zextras.carbonio.chats.meeting.infrastructure.videoserver.JanusService;
import com.zextras.carbonio.chats.meeting.infrastructure.videoserver.impl.VideoServerClient;
import com.zextras.carbonio.chats.meeting.infrastructure.videoserver.impl.VideoServerServiceImpl;
import com.zextras.carbonio.chats.meeting.mapper.MeetingMapper;
import com.zextras.carbonio.chats.meeting.mapper.ParticipantMapper;
import com.zextras.carbonio.chats.meeting.mapper.impl.MeetingMapperImpl;
import com.zextras.carbonio.chats.meeting.mapper.impl.ParticipantMapperImpl;
import com.zextras.carbonio.chats.meeting.repository.MeetingRepository;
import com.zextras.carbonio.chats.meeting.repository.ParticipantRepository;
import com.zextras.carbonio.chats.meeting.repository.impl.EbeanMeetingRepository;
import com.zextras.carbonio.chats.meeting.repository.impl.EbeanParticipantRepository;
import com.zextras.carbonio.chats.meeting.service.MeetingService;
import com.zextras.carbonio.chats.meeting.service.ParticipantService;
import com.zextras.carbonio.chats.meeting.service.impl.MeetingServiceImpl;
import com.zextras.carbonio.chats.meeting.service.impl.ParticipantServiceImpl;
import com.zextras.carbonio.chats.meeting.web.api.MeetingsApiServiceImpl;
import com.zextras.carbonio.chats.meeting.web.api.RoomsApiServiceImpl;
import javax.inject.Singleton;

public class MeetingModule extends AbstractModule {

  @Override
  protected void configure() {
    super.configure();

    bind(RoomsApi.class);
    bind(RoomsApiService.class).to(RoomsApiServiceImpl.class);

    bind(MeetingsApi.class);
    bind(MeetingsApiService.class).to(MeetingsApiServiceImpl.class);
    bind(MeetingService.class).to(MeetingServiceImpl.class);
    bind(MeetingRepository.class).to(EbeanMeetingRepository.class);
    bind(MeetingMapper.class).to(MeetingMapperImpl.class);

    bind(ParticipantService.class).to(ParticipantServiceImpl.class);
    bind(ParticipantRepository.class).to(EbeanParticipantRepository.class);
    bind(ParticipantMapper.class).to(ParticipantMapperImpl.class);

    bind(JanusService.class).to(VideoServerServiceImpl.class);
  }

  @Singleton
  @Provides
  private VideoServerClient getVideoServerClient(AppConfig appConfig) {
    return VideoServerClient.atURL(
      String.format("http://%s:%s",
        appConfig.get(String.class, ConfigName.VIDEO_SERVER_HOST).orElseThrow(),
        appConfig.get(String.class, ConfigName.VIDEO_SERVER_PORT).orElseThrow()
      )
    );
  }
}
