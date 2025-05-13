// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.User;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.profiling.ProfilingService;
import com.zextras.carbonio.chats.core.repository.UserRepository;
import com.zextras.carbonio.chats.core.service.UserService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.UserDto;
import java.util.List;
import java.util.UUID;

@Singleton
public class UserServiceImpl implements UserService {

  private final ProfilingService profilingService;
  private final UserRepository userRepository;

  @Inject
  public UserServiceImpl(ProfilingService profilingService, UserRepository userRepository) {
    this.profilingService = profilingService;
    this.userRepository = userRepository;
  }

  @Override
  public UserDto getUserById(UUID userId, UserPrincipal currentUser) {
    UserDto partialDto =
        profilingService
            .getById(currentUser, userId)
            .map(
                p ->
                    UserDto.create()
                        .id(UUID.fromString(p.getId()))
                        .email(p.getEmail())
                        .name(p.getName()))
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("User %s was not found", userId.toString())));
    userRepository
        .getById(userId.toString())
        .ifPresent(user -> partialDto.statusMessage(user.getStatusMessage()));
    return partialDto;
  }

  @Override
  public List<UserDto> getUsersByIds(List<String> userIds, UserPrincipal currentUser) {
    List<User> users = userRepository.getByIds(userIds);
    return profilingService.getByIds(currentUser, userIds).stream()
        .map(
            p ->
                UserDto.create()
                    .id(UUID.fromString(p.getId()))
                    .email(p.getEmail())
                    .name(p.getName()))
        .map(
            userDto -> {
              users.stream()
                  .filter(u -> u.getId().equals(userDto.getId().toString()))
                  .findFirst()
                  .ifPresent(u -> userDto.statusMessage(u.getStatusMessage()));
              return userDto;
            })
        .toList();
  }

  @Override
  public boolean userExists(UUID userId, UserPrincipal currentUser) {
    return profilingService.getById(currentUser, userId).isPresent();
  }
}
