package com.zextras.carbonio.chats.it.Utils;

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
import com.zextras.carbonio.chats.core.utils.Utils;
import com.zextras.carbonio.chats.it.Utils.MockedFiles.FileMock;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;

public class IntegrationTestUtils {

  private final RoomRepository             roomRepository;
  private final FileMetadataRepository     fileMetadataRepository;
  private final UserRepository             userRepository;
  private final RoomUserSettingsRepository roomUserSettingsRepository;

  @Inject
  public IntegrationTestUtils(
    RoomRepository roomRepository, FileMetadataRepository fileMetadataRepository, UserRepository userRepository,
    RoomUserSettingsRepository roomUserSettingsRepository
  ) {
    this.roomRepository = roomRepository;
    this.fileMetadataRepository = fileMetadataRepository;
    this.userRepository = userRepository;
    this.roomUserSettingsRepository = roomUserSettingsRepository;
  }

  public Room generateAndSaveRoom(UUID id, RoomTypeDto type, String name, List<UUID> usersIds) {
    return generateAndSaveRoom(id, type, name, usersIds,
      RoomTypeDto.ONE_TO_ONE.equals(type) ? usersIds : List.of(usersIds.get(0)),
      null, null);
  }

  public Room generateAndSaveRoom(
    UUID id, RoomTypeDto type, String name, List<UUID> usersIds, List<UUID> ownerIds, @Nullable List<UUID> mutedIds,
    @Nullable OffsetDateTime pictureUpdateTimestamp
  ) {
    Room room = Room.create();
    room
      .id(id.toString())
      .name(name)
      .type(type)
      .hash(Utils.encodeUuidHash(id.toString()))
      .subscriptions(usersIds.stream().map(userId ->
        Subscription.create()
          .id(new SubscriptionId(room.getId(), userId.toString()))
          .userId(userId.toString())
          .room(room)
          .joinedAt(OffsetDateTime.now())
      ).collect(Collectors.toList()));
    ownerIds.forEach(ownerId ->
      room.getSubscriptions().stream().filter(subscription ->
        subscription.getUserId().equals(ownerId.toString())).findAny().ifPresent(s ->
        s.owner(true)
      ));
    room.userSettings(new ArrayList<>());
    Optional.ofNullable(mutedIds).ifPresent(ids ->
      mutedIds.forEach(mutedId ->
        room.getUserSettings().add(
          RoomUserSettings.create(room, mutedId.toString())
            .mutedUntil(OffsetDateTime.parse("0001-01-01T00:00:00Z"))
        )));
    Optional.ofNullable(pictureUpdateTimestamp).ifPresent(room::pictureUpdatedAt);

    return roomRepository.insert(room);
  }

  public Optional<Room> getRoomById(UUID roomId) {
    return this.roomRepository.getById(roomId.toString());
  }

  public FileMetadata generateAndSaveFileMetadata(UUID fileId, FileMetadataType fileType, UUID userId, UUID roomId) {
    return fileMetadataRepository.save(
      FileMetadata.create()
        .id(fileId.toString())
        .name("name")
        .originalSize(0L)
        .mimeType("mimetype")
        .type(fileType)
        .userId(userId.toString())
        .roomId(roomId.toString()));
  }

  public FileMetadata generateAndSaveFileMetadata(
    FileMock fileMock, FileMetadataType fileType, UUID userId, UUID roomId
  ) {
    return fileMetadataRepository.save(
      FileMetadata.create()
        .id(fileMock.getId())
        .name(fileMock.getName())
        .originalSize(fileMock.getSize())
        .mimeType(fileMock.getMimeType())
        .type(fileType)
        .userId(userId.toString())
        .roomId(roomId.toString()));
  }

  public List<FileMetadata> getFileMetadataByRoomIdAndType(UUID roomId, FileMetadataType type) {
    return fileMetadataRepository.getByRoomIdAndType(roomId.toString(), type, 1000, null);
  }

  public User generateAndSaveUser(UUID id, String statusMessage, OffsetDateTime lastSeenTimestamp, String hash) {
    return userRepository.insert(
      User.create()
        .id(id.toString())
        .statusMessage(statusMessage)
        .lastSeen(lastSeenTimestamp)
        .hash(hash)
    );
  }

  public Optional<RoomUserSettings> getRoomUserSettings(UUID roomId, UUID userId) {
    return roomUserSettingsRepository.getByRoomIdAndUserId(roomId.toString(), userId.toString());
  }

  public RoomUserSettings setRoomUserSettings(RoomUserSettings roomUserSettings) {
    roomUserSettingsRepository.save(roomUserSettings);
    return roomUserSettings;
  }
}
