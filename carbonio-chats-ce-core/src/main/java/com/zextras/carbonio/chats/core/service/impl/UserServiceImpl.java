// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.zextras.carbonio.chats.core.data.model.UserProfile;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.profiling.ProfilingService;
import com.zextras.carbonio.chats.core.repository.UserRepository;
import com.zextras.carbonio.chats.core.service.UserService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.UserDto;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UserServiceImpl implements UserService {

  private final ProfilingService profilingService;
  private final UserRepository   userRepository;

  @Inject
  public UserServiceImpl(ProfilingService profilingService, UserRepository userRepository) {
    this.profilingService = profilingService;
    this.userRepository = userRepository;
  }

  @Override
  public UserDto getUserById(UUID userId, UserPrincipal currentUser) {
    UserDto partialDto = profilingService.getById(currentUser, userId)
      .map(profile -> UserDto.create().id(UUID.fromString(profile.getId())).email(profile.getEmail())
        .name(profile.getName()))
      .orElseThrow(() -> new NotFoundException(String.format("User %s was not found", userId.toString())));
    userRepository.getById(userId.toString()).ifPresent(user -> {
      partialDto.lastSeen(user.getLastSeen().toEpochSecond());
      partialDto.statusMessage(user.getStatusMessage());
    });
    return partialDto;
  }

  @Override
  public boolean userExists(UUID userId, UserPrincipal currentUser) {
    return profilingService.getById(currentUser, userId).isPresent();
  }
}
