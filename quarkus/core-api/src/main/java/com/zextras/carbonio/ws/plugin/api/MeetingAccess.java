// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.api;

import java.util.Optional;
import java.util.UUID;

public interface MeetingAccess {

  Optional<MeetingView> find(UUID meetingId);

  /**
   * @throws jakarta.ws.rs.NotFoundException meeting missing, user not a participant, or user not a
   *     room member
   * @throws jakarta.ws.rs.BadRequestException meeting not active
   * @throws jakarta.ws.rs.ForbiddenException user is not a room owner
   */
  MeetingView requireActiveMeetingModerator(UUID meetingId, UserPrincipal user);
}
