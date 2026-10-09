// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.module;

import com.google.inject.AbstractModule;
import com.zextras.carbonio.chats.api.RoomsApi;
import com.zextras.carbonio.chats.api.RoomsApiService;
import com.zextras.carbonio.chats.core.mapper.RoomMapper;
import com.zextras.carbonio.chats.core.mapper.SubscriptionMapper;
import com.zextras.carbonio.chats.core.mapper.impl.RoomMapperImpl;
import com.zextras.carbonio.chats.core.mapper.impl.SubscriptionMapperImpl;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.core.repository.RoomUserSettingsRepository;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanRoomRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanRoomUserSettingsRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanSubscriptionRepository;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.service.impl.MembersServiceImpl;
import com.zextras.carbonio.chats.core.service.impl.RoomServiceImpl;
import com.zextras.carbonio.chats.core.web.api.RoomsApiServiceImpl;

public class RoomModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(RoomsApi.class);
    bind(RoomsApiService.class).to(RoomsApiServiceImpl.class);
    bind(RoomService.class).to(RoomServiceImpl.class);
    bind(RoomRepository.class).to(EbeanRoomRepository.class);
    bind(RoomMapper.class).to(RoomMapperImpl.class);
    bind(RoomUserSettingsRepository.class).to(EbeanRoomUserSettingsRepository.class);

    bind(MembersService.class).to(MembersServiceImpl.class);
    bind(SubscriptionRepository.class).to(EbeanSubscriptionRepository.class);
    bind(SubscriptionMapper.class).to(SubscriptionMapperImpl.class);
  }
}
