package com.zextras.chats.core.api;

import com.zextras.chats.core.api.*;
import com.zextras.chats.core.model.*;



import java.util.List;
import com.zextras.chats.core.api.NotFoundException;

import java.io.InputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen")
public interface HealthcheckApiService {
      Response healthcheck(SecurityContext securityContext)
      throws NotFoundException;
}
