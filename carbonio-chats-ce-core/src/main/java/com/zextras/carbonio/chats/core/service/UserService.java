// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service;


import com.zextras.carbonio.chats.api.NotFoundException;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.UserDto;
import java.io.File;
import java.util.UUID;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

public interface UserService {
  /**
   * Retrieves info about a user
   *
   * @param userId      the requested user's {@link UUID}
   * @param currentUser the currently authenticated {@link UserPrincipal}
   * @return the requested {@link UserDto}
   **/
  UserDto getUserById(UUID userId, UserPrincipal currentUser);

  /**
   * Checks if a user exists. Current implementations checks the {@link com.zextras.carbonio.chats.core.infrastructure.profiling.ProfilingService}
   * to check if the user exists.
   *
   * @param userId the user whose existance we need to check
   * @param currentUser the current authenticated user
   * @return a {@link Boolean} which indicates if the user exists or not
   */
  boolean userExists(UUID userId, UserPrincipal currentUser);

  /**
   * Gets the user picture
   *
   * @param userId      user identifier
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return The user picture
   */
  FileContentAndMetadata getUserPicture(UUID userId, UserPrincipal currentUser);

  /**
   * Sets a new room picture
   *
   * @param userId      room identifier {@link UUID }
   * @param image       image to set {@link File}
   * @param mimeType    image mime type
   * @param fileName    image file name
   * @param currentUser current authenticated user {@link UserPrincipal}
   **/
  void setUserPicture(UUID userId, File image, String mimeType, String fileName, UserPrincipal currentUser);
}
