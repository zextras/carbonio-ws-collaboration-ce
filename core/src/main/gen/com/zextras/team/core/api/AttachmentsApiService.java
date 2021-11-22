package com.zextras.team.core.api;


import com.zextras.team.core.model.AttachmentDto;
import java.io.File;
import java.util.UUID;
import java.util.List;
import javax.annotation.Generated;
import javax.ws.rs.core.SecurityContext;

@Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen", date = "2021-11-22T10:48:21.557692+01:00[Europe/Rome]")
public interface AttachmentsApiService {

  /**
   * Deletes an uploaded attachment
   *
   * @param fileId file identifier {@link UUID }
   * @param securityContext security context {@link SecurityContext}
  **/
  void deleteAttachment(UUID fileId, SecurityContext securityContext);

  /**
   * Retrieves an uploaded attachment
   *
   * @param fileId file identifier {@link UUID }
   * @param securityContext security context {@link SecurityContext}
   * @return The requested file {@link File }
  **/
  File getAttachment(UUID fileId, SecurityContext securityContext);

  /**
   * Retrieves info related to an uploaded attachment
   *
   * @param fileId file identifier {@link UUID }
   * @param securityContext security context {@link SecurityContext}
   * @return Attachment informations {@link AttachmentDto }
  **/
  AttachmentDto getAttachmentInfo(UUID fileId, SecurityContext securityContext);

  /**
   * Retrieves the prefiew of an uploaded attachment
   *
   * @param fileId file identifier {@link UUID }
   * @param securityContext security context {@link SecurityContext}
   * @return The requested file preview {@link File }
  **/
  File getAttachmentPreview(UUID fileId, SecurityContext securityContext);

}
