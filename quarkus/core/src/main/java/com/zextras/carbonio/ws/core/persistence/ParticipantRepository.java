// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ParticipantRepository implements PanacheRepositoryBase<ParticipantEntity, ParticipantId> {

  public List<ParticipantEntity> listByMeeting(String meetingId) {
    return list("meetingId", meetingId);
  }

  public Optional<ParticipantEntity> find(String meetingId, String userId) {
    return findByIdOptional(new ParticipantId(meetingId, userId));
  }

  public long deleteByMeeting(String meetingId) {
    return delete("meetingId", meetingId);
  }
}
