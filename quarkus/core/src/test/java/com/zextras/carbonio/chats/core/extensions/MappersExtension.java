// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.extensions;

import com.zextras.carbonio.chats.core.mapper.AttachmentMapper;
import com.zextras.carbonio.chats.core.mapper.MeetingMapper;
import com.zextras.carbonio.chats.core.mapper.ParticipantMapper;
import com.zextras.carbonio.chats.core.mapper.RoomMapper;
import com.zextras.carbonio.chats.core.mapper.SubscriptionMapper;
import com.zextras.carbonio.chats.core.mapper.impl.AttachmentMapperImpl;
import com.zextras.carbonio.chats.core.mapper.impl.MeetingMapperImpl;
import com.zextras.carbonio.chats.core.mapper.impl.ParticipantMapperImpl;
import com.zextras.carbonio.chats.core.mapper.impl.RoomMapperImpl;
import com.zextras.carbonio.chats.core.mapper.impl.SubscriptionMapperImpl;
import java.util.Map;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/** Resolves the real mapper implementations as test parameters (was a Guice injector in CE). */
public class MappersExtension implements ParameterResolver {

  private static final Map<Class<?>, Object> MAPPERS;

  static {
    SubscriptionMapper subscriptionMapper = new SubscriptionMapperImpl();
    ParticipantMapper participantMapper = new ParticipantMapperImpl();
    MAPPERS =
        Map.of(
            SubscriptionMapper.class, subscriptionMapper,
            ParticipantMapper.class, participantMapper,
            RoomMapper.class, new RoomMapperImpl(subscriptionMapper),
            MeetingMapper.class, new MeetingMapperImpl(participantMapper),
            AttachmentMapper.class, new AttachmentMapperImpl());
  }

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return MAPPERS.containsKey(parameterContext.getParameter().getType());
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return MAPPERS.get(parameterContext.getParameter().getType());
  }
}
