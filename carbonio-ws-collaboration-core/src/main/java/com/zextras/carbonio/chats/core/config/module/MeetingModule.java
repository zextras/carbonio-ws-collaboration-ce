// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.module;

import com.google.inject.AbstractModule;
import com.zextras.carbonio.chats.api.MeetingsApiService;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.impl.VideoServerServiceImpl;
import com.zextras.carbonio.chats.core.mapper.MeetingMapper;
import com.zextras.carbonio.chats.core.mapper.impl.MeetingMapperImpl;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.service.impl.MeetingServiceImpl;
import com.zextras.carbonio.chats.core.service.impl.ParticipantServiceImpl;
import com.zextras.carbonio.chats.core.web.api.MeetingsApiServiceImpl;
import com.zextras.carbonio.chats.core.web.socket.EventsWebSocketManager;

// Replaceable as a whole by a downstream product: only bindings it changes belong here.
public class MeetingModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(MeetingService.class).to(MeetingServiceImpl.class);
    bind(MeetingsApiService.class).to(MeetingsApiServiceImpl.class);
    bind(ParticipantService.class).to(ParticipantServiceImpl.class);
    bind(VideoServerService.class).to(VideoServerServiceImpl.class);
    bind(MeetingMapper.class).to(MeetingMapperImpl.class);
    bind(EventsWebSocketManager.class);
  }
}
