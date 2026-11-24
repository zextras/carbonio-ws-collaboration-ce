// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.module;

import static com.zextras.carbonio.chats.core.config.module.CoreModule.URL_PATTERN;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.api.MeetingsApi;
import com.zextras.carbonio.chats.api.MeetingsApiService;
import com.zextras.carbonio.chats.core.cache.CacheVideoServerSession;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerClient;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerConfig;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.impl.VideoServerConfigImpl;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.impl.VideoServerHttpClient;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.impl.VideoServerServiceImpl;
import com.zextras.carbonio.chats.core.mapper.MeetingMapper;
import com.zextras.carbonio.chats.core.mapper.ParticipantMapper;
import com.zextras.carbonio.chats.core.mapper.impl.MeetingMapperImpl;
import com.zextras.carbonio.chats.core.mapper.impl.ParticipantMapperImpl;
import com.zextras.carbonio.chats.core.repository.MeetingRepository;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository;
import com.zextras.carbonio.chats.core.repository.VideoServerMeetingRepository;
import com.zextras.carbonio.chats.core.repository.VideoServerSessionRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanMeetingRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanParticipantRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanVideoServerMeetingRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanVideoServerSessionRepository;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.service.impl.MeetingServiceImpl;
import com.zextras.carbonio.chats.core.service.impl.ParticipantServiceImpl;
import com.zextras.carbonio.chats.core.web.api.MeetingsApiServiceImpl;
import com.zextras.carbonio.chats.core.web.socket.VideoServerEventListener;
import com.zextras.carbonio.chats.core.web.utility.HttpClient;

public class MeetingModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(MeetingsApi.class);
    bind(MeetingsApiService.class).to(meetingsApiService());
    bind(MeetingService.class).to(meetingService());
    bind(MeetingMapper.class).to(meetingMapper());
    bind(MeetingRepository.class).to(EbeanMeetingRepository.class);

    bind(ParticipantService.class).to(participantService());
    bind(ParticipantRepository.class).to(EbeanParticipantRepository.class);
    bind(ParticipantMapper.class).to(ParticipantMapperImpl.class);

    bind(VideoServerService.class).to(videoServerService());
    bind(VideoServerMeetingRepository.class).to(EbeanVideoServerMeetingRepository.class);
    bind(VideoServerSessionRepository.class).to(EbeanVideoServerSessionRepository.class);
    bind(VideoServerEventListener.class);
    bind(CacheVideoServerSession.class);
  }

  protected Class<? extends MeetingsApiService> meetingsApiService() {
    return MeetingsApiServiceImpl.class;
  }

  protected Class<? extends MeetingService> meetingService() {
    return MeetingServiceImpl.class;
  }

  protected Class<? extends MeetingMapper> meetingMapper() {
    return MeetingMapperImpl.class;
  }

  protected Class<? extends ParticipantService> participantService() {
    return ParticipantServiceImpl.class;
  }

  protected Class<? extends VideoServerService> videoServerService() {
    return VideoServerServiceImpl.class;
  }

  @Singleton
  @Provides
  private VideoServerClient getVideoServerClient(
      AppConfig appConfig, HttpClient httpClient, ObjectMapper objectMapper) {
    return new VideoServerHttpClient(
        httpClient,
        String.format(
            URL_PATTERN,
            appConfig.get(String.class, ConfigName.VIDEO_SERVER_HOST).orElseThrow(),
            appConfig.get(String.class, ConfigName.VIDEO_SERVER_PORT).orElseThrow()),
        objectMapper);
  }

  @Singleton
  @Provides
  private VideoServerConfig getVideoServerConfig(AppConfig appConfig) {
    return new VideoServerConfigImpl()
        .apiSecret(appConfig.get(String.class, ConfigName.VIDEO_SERVER_TOKEN).orElse(null))
        .bitrate(appConfig.get(Integer.class, ConfigName.VIDEO_ROOM_BITRATE).orElse(8000000))
        .bitrateCap(appConfig.get(Boolean.class, ConfigName.VIDEO_ROOM_BITRATE_CAP).orElse(true));
  }
}
