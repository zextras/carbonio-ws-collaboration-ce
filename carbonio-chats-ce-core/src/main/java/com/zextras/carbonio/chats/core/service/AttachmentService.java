package com.zextras.carbonio.chats.core.service;

import com.zextras.carbonio.chats.core.data.model.AttachmentFile;
import com.zextras.carbonio.chats.core.model.AttachmentDto;
import com.zextras.carbonio.chats.core.model.IdDto;
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
  AttachmentFile getAttachmentById(UUID fileId, MockUserPrincipal currentUser);

  /**
   * Retrieves the preview of an uploaded attachment
   *
   * @param fileId      file identifier {@link UUID}
   * @param currentUser security context {@link MockUserPrincipal}
   * @return The requested file preview {@link File}
   **/
  AttachmentFile getAttachmentPreviewById(UUID fileId, MockUserPrincipal currentUser);

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
