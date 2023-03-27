// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.data.model.PaginationFilter;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.AttachmentDto;
import com.zextras.carbonio.chats.model.AttachmentsPaginationDto;
import com.zextras.carbonio.chats.model.IdDto;
import java.io.File;
import java.util.UUID;
import javax.annotation.Nullable;

public interface AttachmentService {

  /**
   * Gets a room's attachment
   *
   * @param fileId      identifier of attachment file to delete {@link UUID}
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return The attachment file requested {@link File}
   */
  FileContentAndMetadata getAttachmentById(UUID fileId, UserPrincipal currentUser);

  /**
   * Retrieves the preview of an uploaded attachment
   *
   * @param fileId      file identifier {@link UUID}
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return The requested file preview {@link File}
   **/
  File getAttachmentPreviewById(UUID fileId, UserPrincipal currentUser);

  /**
   * Retrieves paged list of metadata of every attachment uploaded to the room and the filter for the next page
   *
   * @param roomId      room identifier {@link UUID}
   * @param itemsNumber items number for a page
   * @param filter      base64 encoded string of a json-serialized {@link PaginationFilter}
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return paged list of attachments metadata for the requested room
   */
  AttachmentsPaginationDto getAttachmentInfoByRoomId(
    UUID roomId, Integer itemsNumber, @Nullable String filter, UserPrincipal currentUser
  );

  /**
   * Retrieves info related to an uploaded attachment
   *
   * @param fileId      file identifier {@link UUID}
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return Attachment information {@link AttachmentDto}
   **/
  AttachmentDto getAttachmentInfoById(UUID fileId, UserPrincipal currentUser);

  /**
   * Saves a room's attachment file
   *
   * @param roomId      identifier of the room attachment {@link UUID}
   * @param file        file to save {@link File}
   * @param mimeType    file mime type
   * @param fileName    file name
   * @param description file description
   * @param messageId   identifier of XMPP message to create
   * @param replyId     identifier of the message being replied to
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return The added file identifier {@link IdDto}
   */
  IdDto addAttachment(
    UUID roomId, File file, String mimeType, String fileName, String description, @Nullable String messageId,
    @Nullable String replyId, UserPrincipal currentUser
  );

  /**
   * Copies an attachment in a destinationRoom
   *
   * @param destinationRoom      destination room {@link Room}
   * @param originalAttachmentId identifier of the attachment to copy {@link UUID}
   * @param currentUser          current authenticated user {@link UserPrincipal}
   * @return The new file data {@link FileMetadata}
   */
  FileMetadata copyAttachment(Room destinationRoom, UUID originalAttachmentId, UserPrincipal currentUser);

  /**
   * Deletes a room's attachment file
   *
   * @param fileId      identifier of attachment file to delete {@link UUID}
   * @param currentUser current authenticated user {@link UserPrincipal}
   */
  void deleteAttachment(UUID fileId, UserPrincipal currentUser);

  /**
   * Deletes all room attachments
   *
   * @param roomId      room identifier {@link UUID}
   * @param currentUser current authenticated user {@link UserPrincipal}
   */
  void deleteAttachmentsByRoomId(UUID roomId, UserPrincipal currentUser);
}
