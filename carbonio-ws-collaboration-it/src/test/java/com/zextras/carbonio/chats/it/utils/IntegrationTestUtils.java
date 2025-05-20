// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.utils;

import com.google.inject.Inject;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.entity.SubscriptionId;
import com.zextras.carbonio.chats.core.data.entity.User;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.core.repository.RoomUserSettingsRepository;
import com.zextras.carbonio.chats.core.repository.UserRepository;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import jakarta.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class IntegrationTestUtils {

  private final RoomRepository roomRepository;
  private final FileMetadataRepository fileMetadataRepository;
  private final UserRepository userRepository;
  private final RoomUserSettingsRepository roomUserSettingsRepository;

  @Inject
  public IntegrationTestUtils(
      RoomRepository roomRepository,
      FileMetadataRepository fileMetadataRepository,
      UserRepository userRepository,
      RoomUserSettingsRepository roomUserSettingsRepository) {
    this.roomRepository = roomRepository;
    this.fileMetadataRepository = fileMetadataRepository;
    this.userRepository = userRepository;
    this.roomUserSettingsRepository = roomUserSettingsRepository;
  }

  public Room generateAndSaveRoom(
      UUID id, RoomTypeDto type, @Nullable String name, List<UUID> usersIds) {
    return generateAndSaveRoom(
        id,
        type,
        name,
        usersIds,
        RoomTypeDto.ONE_TO_ONE.equals(type) ? usersIds : List.of(usersIds.get(0)),
        null,
        null);
  }

  public Room generateAndSaveRoom(
      UUID id,
      RoomTypeDto type,
      @Nullable String name,
      List<UUID> usersIds,
      List<UUID> ownerIds,
      @Nullable List<UUID> mutedIds,
      @Nullable OffsetDateTime pictureUpdateTimestamp) {
    return generateAndSaveRoom(
        id, type, name, null, usersIds, ownerIds, mutedIds, pictureUpdateTimestamp);
  }

  public Room generateAndSaveRoom(
      UUID id,
      RoomTypeDto type,
      @Nullable String name,
      @Nullable String description,
      List<UUID> usersIds,
      List<UUID> ownerIds,
      @Nullable List<UUID> mutedIds,
      @Nullable OffsetDateTime pictureUpdateTimestamp) {
    Room room = Room.create();
    room.id(id.toString())
        .name(name)
        .description(description)
        .type(type)
        .subscriptions(
            usersIds.stream()
                .map(
                    userId ->
                        Subscription.create()
                            .id(new SubscriptionId(room.getId(), userId.toString()))
                            .userId(userId.toString())
                            .room(room)
                            .joinedAt(OffsetDateTime.now()))
                .collect(Collectors.toList()));
    ownerIds.forEach(
        ownerId ->
            room.getSubscriptions().stream()
                .filter(subscription -> subscription.getUserId().equals(ownerId.toString()))
                .findAny()
                .ifPresent(s -> s.owner(true)));
    room.userSettings(new ArrayList<>());
    Optional.ofNullable(mutedIds)
        .ifPresent(
            ids ->
                mutedIds.forEach(
                    mutedId ->
                        room.getUserSettings()
                            .add(
                                RoomUserSettings.create(room, mutedId.toString())
                                    .mutedUntil(OffsetDateTime.parse("0001-01-01T00:00:00Z")))));
    Optional.ofNullable(pictureUpdateTimestamp).ifPresent(room::pictureUpdatedAt);

    return roomRepository.insert(room);
  }

  public Room generateAndSaveRoom(UUID roomId, RoomTypeDto type, List<RoomMemberField> members) {
    return generateAndSaveRoom(
        Room.create().id(roomId.toString()).name("name").description("description").type(type),
        members);
  }

  public Room generateAndSaveRoom(Room room, List<RoomMemberField> members) {
    room.subscriptions(new ArrayList<>());
    room.userSettings(new ArrayList<>());
    members.forEach(
        member -> {
          room.getSubscriptions()
              .add(
                  Subscription.create()
                      .id(new SubscriptionId(room.getId(), member.getId().toString()))
                      .userId(member.getId().toString())
                      .room(room)
                      .owner(member.isOwner())
                      .joinedAt(OffsetDateTime.now()));

          if (member.isMuted()) {
            room.getUserSettings()
                .add(
                    RoomUserSettings.create(room, member.getId().toString())
                        .mutedUntil(OffsetDateTime.parse("0001-01-01T00:00:00Z")));
          }
        });
    return roomRepository.insert(room);
  }

  public Room updateRoom(Room room) {
    return roomRepository.update(room);
  }

  public static class RoomMemberField {

    private UUID id;
    private boolean owner = false;
    private boolean muted = false;

    public static RoomMemberField create() {
      return new RoomMemberField();
    }

    public UUID getId() {
      return id;
    }

    public RoomMemberField id(UUID id) {
      this.id = id;
      return this;
    }

    public boolean isOwner() {
      return owner;
    }

    public RoomMemberField owner(boolean owner) {
      this.owner = owner;
      return this;
    }

    public boolean isMuted() {
      return muted;
    }

    public RoomMemberField muted(boolean muted) {
      this.muted = muted;
      return this;
    }
  }

  public Optional<Room> getRoomById(UUID roomId) {
    return this.roomRepository.getById(roomId.toString());
  }

  public FileMetadata generateAndSaveFileMetadata(
      UUID fileId,
      String name,
      String mimeType,
      FileMetadataType fileType,
      UUID userId,
      @Nullable UUID roomId) {
    return fileMetadataRepository.save(
        FileMetadata.create()
            .id(fileId.toString())
            .name(name)
            .originalSize(0L)
            .mimeType(mimeType)
            .type(fileType)
            .userId(userId.toString())
            .roomId(roomId == null ? null : roomId.toString()));
  }

  public FileMetadata generateAndSaveFileMetadata(
      MockedFiles.FileMock fileMock,
      FileMetadataType fileType,
      UUID userId,
      @Nullable UUID roomId) {
    return fileMetadataRepository.save(
        FileMetadata.create()
            .id(fileMock.getId())
            .name(fileMock.getName())
            .originalSize(fileMock.getSize())
            .mimeType(fileMock.getMimeType())
            .type(fileType)
            .userId(userId.toString())
            .roomId(roomId == null ? null : roomId.toString()));
  }

  public List<FileMetadata> getFileMetadataByRoomIdAndType(UUID roomId, FileMetadataType type) {
    return fileMetadataRepository.getByRoomIdAndType(roomId.toString(), type, 1000, null);
  }

  public Optional<FileMetadata> getFileMetadataById(UUID fileId) {
    return fileMetadataRepository.getById(fileId.toString());
  }

  public User generateAndSaveUser(UUID id, String statusMessage) {
    return userRepository.save(User.create().id(id.toString()).statusMessage(statusMessage));
  }

  public Optional<RoomUserSettings> getRoomUserSettings(UUID roomId, UUID userId) {
    return roomUserSettingsRepository.getByRoomIdAndUserId(roomId.toString(), userId.toString());
  }

  public RoomUserSettings setRoomUserSettings(RoomUserSettings roomUserSettings) {
    roomUserSettingsRepository.save(roomUserSettings);
    return roomUserSettings;
  }
}
