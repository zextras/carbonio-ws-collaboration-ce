// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.module;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.zextras.carbonio.chats.api.InboxApi;
import com.zextras.carbonio.chats.api.InboxApiService;
import com.zextras.carbonio.chats.api.PresenceApi;
import com.zextras.carbonio.chats.api.PresenceApiService;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageReceivedBroadcaster;
import com.zextras.carbonio.chats.core.mapper.TimelineItemMapper;
import com.zextras.carbonio.chats.core.repository.DatabaseClock;
import com.zextras.carbonio.chats.core.repository.MessagePinRepository;
import com.zextras.carbonio.chats.core.repository.MessageReactionRepository;
import com.zextras.carbonio.chats.core.repository.MessageRepository;
import com.zextras.carbonio.chats.core.repository.MessageSendDedupRepository;
import com.zextras.carbonio.chats.core.repository.MessageWriteRepository;
import com.zextras.carbonio.chats.core.repository.ReadMarkerRepository;
import com.zextras.carbonio.chats.core.repository.SystemEventRepository;
import com.zextras.carbonio.chats.core.repository.TimelineRepository;
import com.zextras.carbonio.chats.core.repository.UserContactRepository;
import com.zextras.carbonio.chats.core.repository.WsPresenceRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanDatabaseClock;
import com.zextras.carbonio.chats.core.repository.impl.EbeanMessagePinRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanMessageReactionRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanMessageRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanMessageSendDedupRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanMessageWriteRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanReadMarkerRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanSystemEventRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanUserContactRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanWsPresenceRepository;
import com.zextras.carbonio.chats.core.repository.impl.TimelineRepositoryImpl;
import com.zextras.carbonio.chats.core.service.ChatExportService;
import com.zextras.carbonio.chats.core.service.ChatMessageService;
import com.zextras.carbonio.chats.core.service.MessageSentListener;
import com.zextras.carbonio.chats.core.service.SystemEventContentFactory;
import com.zextras.carbonio.chats.core.service.impl.ChatExportServiceImpl;
import com.zextras.carbonio.chats.core.service.impl.ChatMessageServiceImpl;
import com.zextras.carbonio.chats.core.web.api.InboxApiServiceImpl;
import com.zextras.carbonio.chats.core.web.api.PresenceApiServiceImpl;
import com.zextras.carbonio.chats.core.web.utility.ChatExportFormatter;

public class ChatModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(InboxApi.class);
    bind(InboxApiService.class).to(InboxApiServiceImpl.class);
    bind(PresenceApi.class);
    bind(PresenceApiService.class).to(PresenceApiServiceImpl.class);

    bind(TimelineItemMapper.class);
    bind(MessageReceivedBroadcaster.class);
    bind(SystemEventContentFactory.class);

    bind(MessageRepository.class).to(EbeanMessageRepository.class);
    bind(MessageWriteRepository.class).to(EbeanMessageWriteRepository.class);
    bind(MessageReactionRepository.class).to(EbeanMessageReactionRepository.class);
    bind(ReadMarkerRepository.class).to(EbeanReadMarkerRepository.class);
    bind(MessagePinRepository.class).to(EbeanMessagePinRepository.class);
    bind(SystemEventRepository.class).to(EbeanSystemEventRepository.class);
    bind(DatabaseClock.class).to(EbeanDatabaseClock.class);
    bind(TimelineRepository.class).to(TimelineRepositoryImpl.class);
    bind(WsPresenceRepository.class).to(EbeanWsPresenceRepository.class);
    bind(UserContactRepository.class).to(EbeanUserContactRepository.class);
    bind(MessageSendDedupRepository.class).to(EbeanMessageSendDedupRepository.class);

    bind(ChatMessageService.class).to(ChatMessageServiceImpl.class);
    bind(ChatExportFormatter.class);
    bind(ChatExportService.class).to(ChatExportServiceImpl.class);

    Multibinder.newSetBinder(binder(), MessageSentListener.class);
  }
}
