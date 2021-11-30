package com.zextras.chats.core.service.impl;

import com.zextras.chats.core.api.AttachmentsApiService;
import com.zextras.chats.core.model.AttachmentDto;
import java.io.File;
import java.util.UUID;
import javax.ws.rs.core.SecurityContext;

public class AttachmentsApiServiceImpl implements AttachmentsApiService {

  @Override
  public void deleteAttachment(UUID fileId, SecurityContext securityContext) {

  }

  @Override
  public File getAttachment(UUID fileId, SecurityContext securityContext) {
    return null;
  }

  @Override
  public AttachmentDto getAttachmentInfo(
    UUID fileId, SecurityContext securityContext
  ) {
    return null;
  }

  @Override
  public File getAttachmentPreview(UUID fileId, SecurityContext securityContext) {
    return null;
  }
}
