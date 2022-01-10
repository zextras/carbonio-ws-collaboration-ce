package com.zextras.carbonio.chats.core.api;

import com.zextras.carbonio.chats.core.api.*;
import com.zextras.carbonio.chats.core.model.*;


import com.zextras.carbonio.chats.core.model.AttachmentDto;
import java.io.File;
import java.util.UUID;

import java.util.List;
import com.zextras.carbonio.chats.core.api.NotFoundException;

import java.io.InputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen")
public interface AttachmentsApiService {
      Response deleteAttachment(UUID fileId,SecurityContext securityContext)
      throws NotFoundException;
      Response getAttachment(UUID fileId,SecurityContext securityContext)
      throws NotFoundException;
      Response getAttachmentInfo(UUID fileId,SecurityContext securityContext)
      throws NotFoundException;
      Response getAttachmentPreview(UUID fileId,SecurityContext securityContext)
      throws NotFoundException;
}
