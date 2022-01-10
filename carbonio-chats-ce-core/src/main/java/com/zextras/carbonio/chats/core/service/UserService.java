package com.zextras.carbonio.chats.core.service;


import com.zextras.carbonio.chats.core.web.security.MockUserPrincipal;
import com.zextras.carbonio.chats.core.model.UserDto;
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
