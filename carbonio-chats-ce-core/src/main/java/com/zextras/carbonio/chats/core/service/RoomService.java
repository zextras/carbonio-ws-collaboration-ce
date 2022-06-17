// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service;

import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.HashDto;
import com.zextras.carbonio.chats.model.RoomCreationFieldsDto;
import com.zextras.carbonio.chats.model.RoomDto;
import com.zextras.carbonio.chats.model.RoomEditableFieldsDto;
import com.zextras.carbonio.chats.model.RoomExtraFieldDto;
import com.zextras.carbonio.chats.model.RoomInfoDto;
import com.zextras.carbonio.chats.model.RoomRankDto;
import java.io.File;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

public interface RoomService {

  /**
   * Get room by identifier and check if the current user is subscribed. This method returns an entity because it's
   * intended to be used only to be called by services.
   *
   * @param roomId      room identifier {@link UUID}
   * @param currentUser current authenticate user {@link UserPrincipal}
   * @param mustBeOwner if true, the user must be a room owner
   * @return The requested room {@link Room}
   */
  Room getRoomAndCheckUser(UUID roomId, UserPrincipal currentUser, boolean mustBeOwner);

  /**
   * Creates a room of the specified type
   *
   * @param insertRoomRequestDto room to create {@link RoomCreationFieldsDto }
   * @param currentUser          current authenticated user {@link UserPrincipal}
   * @return The newly created room {@link RoomInfoDto }
   **/
  RoomInfoDto createRoom(RoomCreationFieldsDto insertRoomRequestDto, UserPrincipal currentUser);

  /**
   * Deletes the specified room
   *
   * @param roomId      room identifier {@link UUID }
   * @param currentUser current authenticate user {@link UserPrincipal}
   **/
  void deleteRoom(UUID roomId, UserPrincipal currentUser);


  /**
   * Retrieves the requested room
   *
   * @param roomId      room identifier {@link UUID }
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return Requested room {@link RoomInfoDto }
   **/
  RoomInfoDto getRoomById(UUID roomId, UserPrincipal currentUser);

  /**
   * Retrieves a list of every room the user has access to
   *
   * @param extraFields
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return List of every room that the user has access to {@link RoomDto }
   **/
  List<RoomDto> getRooms(@Nullable List<RoomExtraFieldDto> extraFields, UserPrincipal currentUser);

  /**
   * Mutes notification for the specified room
   *
   * @param roomId      room identifier {@link UUID }
   * @param currentUser current authenticated user {@link UserPrincipal}
   **/
  void muteRoom(UUID roomId, UserPrincipal currentUser);


  /**
   * Resets the specified room hash
   *
   * @param roomId      room identifier {@link UUID }
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return Room's hash {@link HashDto }
   **/
  HashDto resetRoomHash(UUID roomId, UserPrincipal currentUser);


  /**
   * Unmutes notification for the specified room
   *
   * @param roomId      room identifier {@link UUID }
   * @param currentUser current authenticated user {@link UserPrincipal}
   **/
  void unmuteRoom(UUID roomId, UserPrincipal currentUser);

  /**
   * Updates a room information
   *
   * @param roomId               room identifier {@link UUID }
   * @param updateRoomRequestDto room fields to update {@link RoomEditableFieldsDto }
   * @param currentUser          current authenticated user {@link UserPrincipal}
   * @return Updated room {@link RoomDto }
   **/
  RoomDto updateRoom(UUID roomId, RoomEditableFieldsDto updateRoomRequestDto, UserPrincipal currentUser);

  /**
   * Gets the room picture
   *
   * @param roomId      room identifier
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return The room picture
   */
  FileContentAndMetadata getRoomPicture(UUID roomId, UserPrincipal currentUser);

  /**
   * Sets a new room picture
   *
   * @param roomId      room identifier {@link UUID }
   * @param image       image to set {@link File}
   * @param mimeType    image mime type
   * @param fileName    image file name
   * @param currentUser current authenticated user {@link UserPrincipal}
   **/
  void setRoomPicture(UUID roomId, File image, String mimeType, String fileName, UserPrincipal currentUser);

  /**
   * Updates the workspaces order for the current user
   *
   * @param roomRankDto list of room identifier and room rank
   * @param currentUser current authenticated user {@link UserPrincipal}
   */
  void updateWorkspacesRank(List<RoomRankDto> roomRankDto, UserPrincipal currentUser);
}
