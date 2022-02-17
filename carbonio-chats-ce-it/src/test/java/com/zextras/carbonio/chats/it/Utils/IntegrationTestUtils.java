package com.zextras.carbonio.chats.it.Utils;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.entity.SubscriptionId;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.core.utils.Utils;
import com.zextras.carbonio.chats.it.Utils.MockedFiles.FileMock;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;

public class IntegrationTestUtils {

  private final RoomRepository roomRepository;
  private final FileMetadataRepository fileMetadataRepository;

  @Inject
  public IntegrationTestUtils(RoomRepository roomRepository, FileMetadataRepository fileMetadataRepository) {
    this.roomRepository = roomRepository;
    this.fileMetadataRepository = fileMetadataRepository;
  }

  public Room generateAndSaveRoom(UUID id, RoomTypeDto type, String name, List<UUID> usersIds) {
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
    room.getSubscriptions().get(0).owner(true);
    return roomRepository.insert(room);
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
        .id(fileMock.getId().toString())
        .name(fileMock.getName())
        .originalSize(fileMock.getSize())
        .mimeType(fileMock.getMimeType())
        .type(fileType)
        .userId(userId.toString())
        .roomId(roomId.toString()));
  }
}
