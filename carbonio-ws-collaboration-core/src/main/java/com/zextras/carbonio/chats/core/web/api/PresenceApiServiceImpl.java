// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.api.NotFoundException;
import com.zextras.carbonio.chats.api.PresenceApiService;
import com.zextras.carbonio.chats.core.data.model.PresenceSummary;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.service.ChatMessageService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.PresenceBatchRequestDto;
import com.zextras.carbonio.chats.model.PresenceStatusDto;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class PresenceApiServiceImpl implements PresenceApiService {

  private final ChatMessageService chatMessageService;

  @Inject
  public PresenceApiServiceImpl(ChatMessageService chatMessageService) {
    this.chatMessageService = chatMessageService;
  }

  private static UserPrincipal getCurrentUser(SecurityContext securityContext) {
    return Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
        .orElseThrow(UnauthorizedException::new);
  }

  @Override
  public Response getPresenceBatch(
      PresenceBatchRequestDto presenceBatchRequestDto, SecurityContext securityContext)
      throws NotFoundException {
    getCurrentUser(securityContext);

    List<PresenceStatusDto> result = new ArrayList<>();
    if (presenceBatchRequestDto != null && presenceBatchRequestDto.getUserIds() != null) {
      List<String> ids = presenceBatchRequestDto.getUserIds().stream().map(UUID::toString).toList();
      List<PresenceSummary> summaries = chatMessageService.getBatchPresence(ids);
      for (PresenceSummary s : summaries) {
        PresenceStatusDto dto =
            PresenceStatusDto.create().userId(UUID.fromString(s.userId())).online(s.online());
        if (s.lastSeen() != null) {
          dto.lastActivity(s.lastSeen().withOffsetSameInstant(ZoneOffset.UTC));
        }
        result.add(dto);
      }
    }

    return Response.status(Status.OK).entity(result).build();
  }
}
