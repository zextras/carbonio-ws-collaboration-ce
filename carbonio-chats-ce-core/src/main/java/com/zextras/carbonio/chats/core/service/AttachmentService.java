// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service;

import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.model.AttachmentDto;
import com.zextras.carbonio.chats.model.IdDto;
import com.zextras.carbonio.chats.core.web.security.MockUserPrincipal;
import java.io.File;
import java.util.UUID;

public interface AttachmentService {

  /**
   * Gets a room's attachment
   *
   * @param fileId      identifier of attachment file to delete {@link UUID}
   * @param currentUser current authenticated user {@link MockUserPrincipal}
   * @return The attachment file requested {@link File}
   */
  FileContentAndMetadata getAttachmentById(UUID fileId, MockUserPrincipal currentUser);

  /**
   * Retrieves the preview of an uploaded attachment
   *
   * @param fileId      file identifier {@link UUID}
   * @param currentUser security context {@link MockUserPrincipal}
   * @return The requested file preview {@link File}
   **/
  FileContentAndMetadata getAttachmentPreviewById(UUID fileId, MockUserPrincipal currentUser);

  /**
   * Retrieves info related to an uploaded attachment
   *
   * @param fileId      file identifier {@link UUID}
   * @param currentUser security context {@link MockUserPrincipal}
   * @return Attachment information {@link AttachmentDto}
   **/
  AttachmentDto getAttachmentInfoById(UUID fileId, MockUserPrincipal currentUser);

  /**
   * Saves a room's attachment file
   *
   * @param roomId      identifier of the room attachment {@link UUID}
   * @param file        file to save {@link File}
   * @param mimeType    file mime type
   * @param fileName    file name
   * @param currentUser current authenticated user {@link MockUserPrincipal}
   * @return The added file identifier {@link IdDto}
   */
  IdDto addAttachment(UUID roomId, File file, String mimeType, String fileName, MockUserPrincipal currentUser);

  /**
   * Deletes a room's attachment file
   *
   * @param fileId      identifier of attachment file to delete {@link UUID}
   * @param currentUser current authenticated user {@link MockUserPrincipal}
   */
  void deleteAttachment(UUID fileId, MockUserPrincipal currentUser);
}
