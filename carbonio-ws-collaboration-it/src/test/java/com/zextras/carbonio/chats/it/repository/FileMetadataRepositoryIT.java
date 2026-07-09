// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.model.AttachmentFilter;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.data.type.MimeTypeCategory;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockedAccountType;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
class FileMetadataRepositoryIT {

  private final FileMetadataRepository fileMetadataRepository;
  private final IntegrationTestUtils integrationTestUtils;

  public FileMetadataRepositoryIT(
      FileMetadataRepository fileMetadataRepository, IntegrationTestUtils integrationTestUtils) {
    this.fileMetadataRepository = fileMetadataRepository;
    this.integrationTestUtils = integrationTestUtils;
  }

  private static UUID userId;

  @BeforeAll
  static void initAll() {
    userId = MockedAccount.getAccount(MockedAccountType.SNOOPY).getUUID();
  }

  @Nested
  @DisplayName("Get by room id and type with mime type category filter tests")
  class MimeTypeCategoryFilterTest {

    private UUID seedRoomWithMixedAttachments() {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room", List.of(userId));
      saveAttachment(roomId, "image png", "image/png");
      saveAttachment(roomId, "image jpeg", "image/jpeg");
      saveAttachment(roomId, "video mp4", "video/mp4");
      saveAttachment(roomId, "audio mpeg", "audio/mpeg");
      saveAttachment(roomId, "pdf doc", "application/pdf");
      saveAttachment(roomId, "plain text", "text/plain");
      saveAttachment(roomId, "zip archive", "application/zip");
      return roomId;
    }

    private void saveAttachment(UUID roomId, String name, String mimeType) {
      integrationTestUtils.generateAndSaveFileMetadata(
          UUID.randomUUID(), name, mimeType, FileMetadataType.ATTACHMENT, userId, roomId);
    }

    private Set<String> mimeTypes(List<FileMetadata> metadataList) {
      return metadataList.stream().map(FileMetadata::getMimeType).collect(Collectors.toSet());
    }

    @Test
    @DisplayName("IMAGES category returns only image attachments")
    void getByRoomIdAndType_imagesCategory_returnsOnlyImages() {
      UUID roomId = seedRoomWithMixedAttachments();

      List<FileMetadata> result =
          fileMetadataRepository.getByRoomIdAndType(
              roomId.toString(),
              FileMetadataType.ATTACHMENT,
              100,
              null,
              AttachmentFilter.create().mimeTypeCategory(MimeTypeCategory.IMAGES));

      assertEquals(Set.of("image/png", "image/jpeg"), mimeTypes(result));
    }

    @Test
    @DisplayName("VIDEOS category returns only video attachments")
    void getByRoomIdAndType_videosCategory_returnsOnlyVideos() {
      UUID roomId = seedRoomWithMixedAttachments();

      List<FileMetadata> result =
          fileMetadataRepository.getByRoomIdAndType(
              roomId.toString(),
              FileMetadataType.ATTACHMENT,
              100,
              null,
              AttachmentFilter.create().mimeTypeCategory(MimeTypeCategory.VIDEOS));

      assertEquals(Set.of("video/mp4"), mimeTypes(result));
    }

    @Test
    @DisplayName("DOCUMENTS category returns audio and every non image or video attachment")
    void getByRoomIdAndType_documentsCategory_returnsEverythingElseIncludingAudio() {
      UUID roomId = seedRoomWithMixedAttachments();

      List<FileMetadata> result =
          fileMetadataRepository.getByRoomIdAndType(
              roomId.toString(),
              FileMetadataType.ATTACHMENT,
              100,
              null,
              AttachmentFilter.create().mimeTypeCategory(MimeTypeCategory.DOCUMENTS));

      assertEquals(
          Set.of("audio/mpeg", "application/pdf", "text/plain", "application/zip"),
          mimeTypes(result));
    }

    @Test
    @DisplayName("No category filter returns all attachments")
    void getByRoomIdAndType_noCategory_returnsAll() {
      UUID roomId = seedRoomWithMixedAttachments();

      List<FileMetadata> result =
          fileMetadataRepository.getByRoomIdAndType(
              roomId.toString(), FileMetadataType.ATTACHMENT, 100, null, AttachmentFilter.create());

      assertEquals(7, result.size());
    }

    @Test
    @DisplayName("countByRoomIdAndType with no filter counts all attachments")
    void countByRoomIdAndType_noFilter_countsAll() {
      UUID roomId = seedRoomWithMixedAttachments();

      assertEquals(
          7L,
          fileMetadataRepository.countByRoomIdAndType(
              roomId.toString(), FileMetadataType.ATTACHMENT, AttachmentFilter.create()));
    }

    @Test
    @DisplayName("countByRoomIdAndType with IMAGES category counts only images")
    void countByRoomIdAndType_imagesCategory_countsOnlyImages() {
      UUID roomId = seedRoomWithMixedAttachments();

      assertEquals(
          2L,
          fileMetadataRepository.countByRoomIdAndType(
              roomId.toString(),
              FileMetadataType.ATTACHMENT,
              AttachmentFilter.create().mimeTypeCategory(MimeTypeCategory.IMAGES)));
    }

    @Test
    @DisplayName("countByRoomIdAndType with VIDEOS category counts only videos")
    void countByRoomIdAndType_videosCategory_countsOnlyVideos() {
      UUID roomId = seedRoomWithMixedAttachments();

      assertEquals(
          1L,
          fileMetadataRepository.countByRoomIdAndType(
              roomId.toString(),
              FileMetadataType.ATTACHMENT,
              AttachmentFilter.create().mimeTypeCategory(MimeTypeCategory.VIDEOS)));
    }

    @Test
    @DisplayName(
        "countByRoomIdAndType with DOCUMENTS category counts everything else including audio")
    void countByRoomIdAndType_documentsCategory_countsEverythingElse() {
      UUID roomId = seedRoomWithMixedAttachments();

      assertEquals(
          4L,
          fileMetadataRepository.countByRoomIdAndType(
              roomId.toString(),
              FileMetadataType.ATTACHMENT,
              AttachmentFilter.create().mimeTypeCategory(MimeTypeCategory.DOCUMENTS)));
    }
  }
}
