// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.model.AttachmentFilter;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.AttachmentDto;
import com.zextras.carbonio.chats.model.AttachmentsPaginationDto;
import com.zextras.carbonio.chats.model.BulkDeleteAttachmentsResponseDto;
import com.zextras.carbonio.chats.model.IdDto;
import jakarta.annotation.Nullable;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public interface AttachmentService {

  /**
   * Gets a room's attachment
   *
   * @param fileId identifier of attachment file to delete {@link UUID}
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return Content and meta data of yhe attachment file requested {@link FileContentAndMetadata}
   */
  FileContentAndMetadata getAttachmentById(UUID fileId, UserPrincipal currentUser);

  /**
   * Retrieves paged list of metadata of every attachment uploaded to the room and the cursor for
   * the next page
   *
   * @param roomId room identifier {@link UUID}
   * @param limit maximum number of items to return per page
   * @param cursor opaque base64-encoded pagination token from the previous response
   * @param attachmentFilter optional field filters and sort options {@link AttachmentFilter}
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return paged list of attachments metadata for the requested room
   */
  AttachmentsPaginationDto getAttachmentInfoByRoomId(
      UUID roomId,
      Integer limit,
      @Nullable String cursor,
      @Nullable AttachmentFilter attachmentFilter,
      UserPrincipal currentUser);

  /**
   * Retrieves info related to an uploaded attachment
   *
   * @param fileId file identifier {@link UUID}
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return Attachment information {@link AttachmentDto}
   */
  AttachmentDto getAttachmentInfoById(UUID fileId, UserPrincipal currentUser);

  /**
   * Saves a room's attachment file
   *
   * @param roomId identifier of the room attachment {@link UUID}
   * @param file file stream to save {@link InputStream}
   * @param mimeType file mime type
   * @param contentLength file content length
   * @param fileName file name
   * @param description file description
   * @param messageId identifier of XMPP message to create
   * @param replyId identifier of the message being replied to
   * @param area attachment's area
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return The added file identifier {@link IdDto}
   */
  IdDto addAttachment(
      UUID roomId,
      InputStream file,
      String mimeType,
      Long contentLength,
      String fileName,
      String description,
      @Nullable String messageId,
      @Nullable String replyId,
      @Nullable String area,
      UserPrincipal currentUser);

  /**
   * Copies an attachment in a destinationRoom
   *
   * @param destinationRoom destination room {@link Room}
   * @param originalAttachmentId identifier of the attachment to copy {@link UUID}
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return The new file data {@link FileMetadata}
   */
  FileMetadata copyAttachment(
      Room destinationRoom, UUID originalAttachmentId, UserPrincipal currentUser);

  /**
   * Deletes a room's attachment file
   *
   * @param fileId identifier of attachment file to delete {@link UUID}
   * @param currentUser current authenticated user {@link UserPrincipal}
   */
  void deleteAttachment(UUID fileId, UserPrincipal currentUser);

  /**
   * Deletes all room attachments
   *
   * @param roomId room identifier {@link UUID}
   * @param currentUser current authenticated user {@link UserPrincipal}
   */
  void deleteAttachmentsByRoomId(UUID roomId, UserPrincipal currentUser);

  /**
   * Bulk deletes a list of attachments scoped to a specific room. The requesting user must be a
   * room member and must own each attachment. Partial success is supported: attachments that fail
   * to delete from storage keep their metadata and can be retried.
   *
   * @param roomId room identifier {@link UUID}
   * @param fileIds list of attachment file identifiers to delete {@link UUID}
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return result containing successfully deleted and failed attachment identifiers
   */
  BulkDeleteAttachmentsResponseDto bulkDeleteRoomAttachments(
      UUID roomId, List<UUID> fileIds, UserPrincipal currentUser);
}
