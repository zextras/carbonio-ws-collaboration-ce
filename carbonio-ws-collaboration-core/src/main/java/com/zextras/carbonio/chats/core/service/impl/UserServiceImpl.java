// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ChatsConstant.CONFIGURATIONS_DEFAULT_VALUES;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.User;
import com.zextras.carbonio.chats.core.data.event.UserPictureChanged;
import com.zextras.carbonio.chats.core.data.event.UserPictureDeleted;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.profiling.ProfilingService;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import com.zextras.carbonio.chats.core.repository.UserRepository;
import com.zextras.carbonio.chats.core.service.UserService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.UserDto;
import java.io.InputStream;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class UserServiceImpl implements UserService {

  private final ProfilingService profilingService;
  private final UserRepository userRepository;
  private final FileMetadataRepository fileMetadataRepository;
  private final StoragesService storagesService;
  private final SubscriptionRepository subscriptionRepository;
  private final EventDispatcher eventDispatcher;
  private final AppConfig appConfig;
  private final Clock clock;

  @Inject
  public UserServiceImpl(
      ProfilingService profilingService,
      UserRepository userRepository,
      FileMetadataRepository fileMetadataRepository,
      StoragesService storagesService,
      SubscriptionRepository subscriptionRepository,
      EventDispatcher eventDispatcher,
      AppConfig appConfig,
      Clock clock) {
    this.profilingService = profilingService;
    this.userRepository = userRepository;
    this.fileMetadataRepository = fileMetadataRepository;
    this.storagesService = storagesService;
    this.eventDispatcher = eventDispatcher;
    this.subscriptionRepository = subscriptionRepository;
    this.appConfig = appConfig;
    this.clock = clock;
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
        .ifPresent(
            user -> {
              partialDto.pictureUpdatedAt(user.getPictureUpdatedAt());
              partialDto.statusMessage(user.getStatusMessage());
            });
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
                  .ifPresent(
                      u -> {
                        userDto.pictureUpdatedAt(u.getPictureUpdatedAt());
                        userDto.statusMessage(u.getStatusMessage());
                      });
              return userDto;
            })
        .toList();
  }

  @Override
  public boolean userExists(UUID userId, UserPrincipal currentUser) {
    return profilingService.getById(currentUser, userId).isPresent();
  }

  @Override
  public FileContentAndMetadata getUserPicture(UUID userId, UserPrincipal currentUser) {
    FileMetadata metadata =
        fileMetadataRepository
            .find(userId.toString(), null, FileMetadataType.USER_AVATAR)
            .orElseThrow(
                () -> new NotFoundException(String.format("User picture '%s' not found", userId)));
    return new FileContentAndMetadata(
        storagesService.getFileStreamById(metadata.getId(), metadata.getUserId()), metadata);
  }

  @Override
  public void setUserPicture(
      UUID userId,
      InputStream image,
      String mimeType,
      Long contentLength,
      String fileName,
      UserPrincipal currentUser) {
    if (!currentUser.getUUID().equals(userId)) {
      throw new ForbiddenException("The picture can be change only from its owner");
    }
    Integer maxImageSizeKb =
        appConfig
            .get(Integer.class, ConfigName.MAX_USER_IMAGE_SIZE_IN_KB)
            .orElse(CONFIGURATIONS_DEFAULT_VALUES.MAX_USER_IMAGE_SIZE_IN_KB);
    if (contentLength > maxImageSizeKb * 1024) {
      throw new BadRequestException(
          String.format("The user picture cannot be greater than %d kB", maxImageSizeKb));
    }
    if (!mimeType.startsWith("image/")) {
      throw new BadRequestException("The user picture must be an image");
    }
    Optional<FileMetadata> oldMetadata =
        fileMetadataRepository.find(userId.toString(), null, FileMetadataType.USER_AVATAR);
    String fileId =
        oldMetadata.isPresent() ? oldMetadata.get().getId() : UUID.randomUUID().toString();
    storagesService.saveFile(image, fileId, userId.toString(), contentLength);
    fileMetadataRepository.save(
        oldMetadata
            .orElseGet(() -> FileMetadata.create().id(fileId))
            .type(FileMetadataType.USER_AVATAR)
            .name(fileName)
            .originalSize(contentLength)
            .mimeType(mimeType)
            .userId(userId.toString()));
    User savedUser =
        userRepository.save(
            userRepository
                .getById(userId.toString())
                .orElseGet(() -> User.create().id(userId.toString()))
                .pictureUpdatedAt(OffsetDateTime.ofInstant(clock.instant(), clock.getZone())));
    eventDispatcher.sendToUserExchange(
        subscriptionRepository.getContacts(userId.toString()),
        UserPictureChanged.create()
            .userId(userId)
            .imageId(UUID.fromString(fileId))
            .updatedAt(savedUser.getPictureUpdatedAt()));
  }

  @Override
  public void deleteUserPicture(UUID userId, UserPrincipal currentUser) {
    if (!currentUser.getUUID().equals(userId)) {
      throw new ForbiddenException("The picture can be removed only from its owner");
    }
    FileMetadata metadata =
        fileMetadataRepository
            .find(userId.toString(), null, FileMetadataType.USER_AVATAR)
            .orElseThrow(
                () -> new NotFoundException(String.format("User picture '%s' not found", userId)));
    storagesService.deleteFile(metadata.getId(), metadata.getUserId());
    fileMetadataRepository.delete(metadata);
    userRepository
        .getById(userId.toString())
        .ifPresent(user -> userRepository.save(user.pictureUpdatedAt(null)));
    eventDispatcher.sendToUserExchange(
        subscriptionRepository.getContacts(userId.toString()),
        UserPictureDeleted.create().userId(userId));
  }
}
