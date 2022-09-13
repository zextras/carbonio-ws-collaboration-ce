// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.zextras.carbonio.chats.core.config.ChatsConstant;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.event.UserPictureChangedEvent;
import com.zextras.carbonio.chats.core.data.event.UserPictureDeletedEvent;
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
import io.ebean.annotation.Transactional;
import java.io.File;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UserServiceImpl implements UserService {

  private final ProfilingService       profilingService;
  private final UserRepository         userRepository;
  private final FileMetadataRepository fileMetadataRepository;
  private final StoragesService        storagesService;
  private final SubscriptionRepository subscriptionRepository;
  private final EventDispatcher        eventDispatcher;

  @Inject
  public UserServiceImpl(
    ProfilingService profilingService, UserRepository userRepository,
    FileMetadataRepository fileMetadataRepository,
    StoragesService storagesService,
    SubscriptionRepository subscriptionRepository,
    EventDispatcher eventDispatcher
  ) {
    this.profilingService = profilingService;
    this.userRepository = userRepository;
    this.fileMetadataRepository = fileMetadataRepository;
    this.storagesService = storagesService;
    this.eventDispatcher = eventDispatcher;
    this.subscriptionRepository = subscriptionRepository;
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

  @Override
  public FileContentAndMetadata getUserPicture(UUID userId, UserPrincipal currentUser) {
    FileMetadata metadata = fileMetadataRepository.getById(userId.toString())
      .orElseThrow(() -> new NotFoundException(String.format("File with id '%s' not found", userId)));
    File file = storagesService.getFileById(metadata.getId(), metadata.getUserId());
    return new FileContentAndMetadata(file, metadata);
  }

  @Override
  @Transactional
  public void setUserPicture(UUID userId, File image, String mimeType, String fileName, UserPrincipal currentUser) {
    if (!currentUser.getUUID().equals(userId)) {
      throw new ForbiddenException("The picture can be change only from its owner");
    }
    if (image.length() > ChatsConstant.MAX_USER_IMAGE_SIZE_IN_KB * 1024) {
      throw new BadRequestException(
        String.format("The user picture cannot be greater than %d KB", ChatsConstant.MAX_USER_IMAGE_SIZE_IN_KB));
    }
    if (!mimeType.startsWith("image/")) {
      throw new BadRequestException("The user picture must be an image");
    }
    Optional<FileMetadata> oldMetadata = fileMetadataRepository.getById(userId.toString());
    FileMetadata metadata = oldMetadata.orElseGet(() -> FileMetadata.create()
        .id(userId.toString())
        .type(FileMetadataType.USER_AVATAR)
      )
      .name(fileName)
      .originalSize(image.length())
      .mimeType(mimeType)
      .userId(currentUser.getId());
    fileMetadataRepository.save(metadata);
    storagesService.saveFile(image, metadata, currentUser.getId());
    eventDispatcher.sendToUserQueue(
      subscriptionRepository.getContacts(userId.toString()),
      UserPictureChangedEvent.create(currentUser.getUUID()).userId(userId));
  }

  @Override
  @Transactional
  public void deleteUserPicture(UUID userId, UserPrincipal currentUser) {
    if (!currentUser.getUUID().equals(userId) && !currentUser.isSystemUser()) {
      throw new ForbiddenException("The picture can be remove only from its owner");
    }
    FileMetadata metadata = fileMetadataRepository.getById(userId.toString())
      .orElseThrow(() -> new NotFoundException(String.format("File with id '%s' not found", userId)));
    fileMetadataRepository.delete(metadata);
    storagesService.deleteFile(metadata.getId(), metadata.getUserId());
    eventDispatcher.sendToUserQueue(
      subscriptionRepository.getContacts(userId.toString()),
      UserPictureDeletedEvent.create(currentUser.getUUID()).userId(userId));
  }
}
