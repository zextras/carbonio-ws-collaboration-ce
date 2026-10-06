// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.recording;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RecordingRepository implements PanacheRepositoryBase<RecordingEntity, String> {

  public Optional<RecordingEntity> findStarted(String meetingId) {
    return find("meetingId = ?1 and status = ?2", meetingId, RecordingStatus.STARTED).firstResultOptional();
  }

  public List<RecordingEntity> findStarted(Collection<String> meetingIds) {
    if (meetingIds.isEmpty()) {
      return List.of();
    }
    return list("meetingId in ?1 and status = ?2", meetingIds, RecordingStatus.STARTED);
  }
}
