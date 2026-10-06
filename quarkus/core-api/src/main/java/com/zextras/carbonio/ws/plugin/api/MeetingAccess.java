// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Read access to core meetings for plugins.
 *
 * <p><strong>Rollback-only pitfall.</strong> Plugin code often runs inside core's transaction (e.g.
 * synchronous observers of {@link com.zextras.carbonio.ws.plugin.api.event.MeetingStopping}). When
 * a runtime exception propagates out of <em>any</em> {@code @Transactional} core service or bean
 * method (REQUIRED semantics, the default), the shared transaction is marked rollback-only before
 * the plugin sees the exception. Catching it does not undo that: core's request then fails at
 * commit. Only call core methods whose failure must abort the caller, or call them in a separate
 * transaction ({@code REQUIRES_NEW} / {@code QuarkusTransaction.requiringNew()}) when the failure is
 * meant to be tolerated. {@link VideoServerGateway} is safe to catch: it is not transactional around
 * the Janus call.
 */
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
