package com.zextras.carbonio.chats.core.api;

import com.zextras.carbonio.chats.core.api.*;
import com.zextras.carbonio.chats.core.model.*;


import com.zextras.carbonio.chats.core.model.HealthResponseDto;

import java.util.List;
import com.zextras.carbonio.chats.core.api.NotFoundException;

import java.io.InputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen")
public interface HealthApiService {
      Response healthInfo(SecurityContext securityContext)
      throws NotFoundException;
      Response isLive(SecurityContext securityContext)
      throws NotFoundException;
      Response isReady(SecurityContext securityContext)
      throws NotFoundException;
}
