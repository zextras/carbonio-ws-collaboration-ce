package com.zextras.chats.core.service;


import com.zextras.chats.core.model.UserDto;
import com.zextras.chats.core.web.security.MockUserPrincipal;
import java.util.UUID;
import javax.annotation.Generated;

@Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen")
public interface UserService {

  /**
   * Retrieves a user
   *
   * @param userId      user identifier {@link UUID }
   * @param currentUser current authenticated user {@link MockUserPrincipal}
   * @return Requested user {@link UserDto }
   **/
  UserDto getUserById(UUID userId, MockUserPrincipal currentUser);

}
