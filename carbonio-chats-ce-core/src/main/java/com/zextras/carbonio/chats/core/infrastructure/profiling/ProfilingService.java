package com.zextras.carbonio.chats.core.infrastructure.profiling;

import com.zextras.carbonio.chats.core.data.model.UserProfile;
import com.zextras.carbonio.chats.core.infrastructure.HealthIndicator;
import com.zextras.carbonio.chats.model.UserDto;
import java.util.Optional;
import java.util.UUID;

public interface ProfilingService extends HealthIndicator {

  /**
   * Returns the user identified by the given id.
   *
   * @param userId id of the user to retrieve
   * @return an {@link Optional} which contains {@link UserDto} if it was found, or empty otherwise
   */
  Optional<UserProfile> getById(UUID userId);

}
