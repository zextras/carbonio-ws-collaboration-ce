package com.zextras.carbonio.chats.core.api;

import com.zextras.carbonio.chats.core.api.*;
import com.zextras.carbonio.chats.core.model.*;


import java.util.UUID;
import com.zextras.carbonio.chats.core.model.UserDto;

import java.util.List;
import com.zextras.carbonio.chats.core.api.NotFoundException;

import java.io.InputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen")
public interface UsersApiService {
      Response getUserById(UUID userId,SecurityContext securityContext)
      throws NotFoundException;
}
