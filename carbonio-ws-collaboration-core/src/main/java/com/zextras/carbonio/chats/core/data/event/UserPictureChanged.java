// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;

public class UserPictureChanged extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.USER_PICTURE_CHANGED;

  private UUID userId;

  private UUID imageId;

  private OffsetDateTime updatedAt;

  public UserPictureChanged() {
    super(EVENT_TYPE);
  }

  public static UserPictureChanged create() {
    return new UserPictureChanged();
  }

  public UUID getUserId() {
    return userId;
  }

  public UserPictureChanged userId(UUID userId) {
    this.userId = userId;
    return this;
  }

  public UUID getImageId(){ return imageId;}

  public UserPictureChanged imageId(UUID imageId){
    this.imageId = imageId;
    return this;
  }

  public OffsetDateTime getUpdatedAt(){ return updatedAt;}

  public UserPictureChanged updatedAt(OffsetDateTime updatedAt){
    this.updatedAt = updatedAt;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    UserPictureChanged that = (UserPictureChanged) o;
    return Objects.equals(getUserId(), that.getUserId()) &&
      Objects.equals(getImageId(), that.getImageId()) &&
      Objects.equals(getUpdatedAt(), that.getUpdatedAt());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getUserId());
  }
}
