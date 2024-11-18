// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.ForwardMessageDto;
import com.zextras.carbonio.chats.model.RoomCreationFieldsDto;
import com.zextras.carbonio.chats.model.RoomDto;
import com.zextras.carbonio.chats.model.RoomEditableFieldsDto;
import com.zextras.carbonio.chats.model.RoomExtraFieldDto;
import com.zextras.carbonio.chats.model.RoomRankDto;
import jakarta.annotation.Nullable;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomService {

  /**
   * Retrieves rooms identifiers {@link UUID} {@link List} of every room the user has access to
   *
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return identifier {@link UUID} {@link List} of every room that the user has access to {@link
   *     RoomDto }
   */
  List<UUID> getRoomsIds(UserPrincipal currentUser);

  /**
   * Get room by identifier and check if the current user is subscribed. This method returns an
   * entity because it's intended to be used only to be called by services.
   *
   * @param roomId room identifier {@link UUID}
   * @param currentUser current authenticate user {@link UserPrincipal}
   * @param mustBeOwner if true, the user must be a room owner
   * @return The requested room {@link Room}
   * @throws NotFoundException if the indicated room doesn't exist
   * @throws ForbiddenException if the user isn't a room member
   * @throws ForbiddenException if the user isn't a room owner and mustBeOwner is true
   */
  Room getRoomAndValidateUser(UUID roomId, UserPrincipal currentUser, boolean mustBeOwner);

  /**
   * Gets the room entity for internal usage
   *
   * @param roomId room identifier
   * @return {@link Room} entity
   */
  Optional<Room> getRoom(UUID roomId);

  /**
   * Creates a room of the specified type
   *
   * @param insertRoomRequestDto room to create {@link RoomCreationFieldsDto }
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return The newly created room {@link RoomDto }
   */
  RoomDto createRoom(RoomCreationFieldsDto insertRoomRequestDto, UserPrincipal currentUser);

  /**
   * Deletes the specified room
   *
   * @param roomId room identifier {@link UUID }
   * @param currentUser current authenticate user {@link UserPrincipal}
   */
  void deleteRoom(UUID roomId, UserPrincipal currentUser);

  /**
   * Retrieves the requested room
   *
   * @param roomId room identifier {@link UUID }
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return Requested room {@link RoomDto }
   */
  RoomDto getRoomById(UUID roomId, UserPrincipal currentUser);

  /**
   * Retrieves a list of every room the user has access to
   *
   * @param extraFields
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return List of every room that the user has access to {@link RoomDto }
   */
  List<RoomDto> getRooms(@Nullable List<RoomExtraFieldDto> extraFields, UserPrincipal currentUser);

  /**
   * Mutes notification for the specified room
   *
   * @param roomId room identifier {@link UUID }
   * @param currentUser current authenticated user {@link UserPrincipal}
   */
  void muteRoom(UUID roomId, UserPrincipal currentUser);

  /**
   * Unmutes notification for the specified room
   *
   * @param roomId room identifier {@link UUID }
   * @param currentUser current authenticated user {@link UserPrincipal}
   */
  void unmuteRoom(UUID roomId, UserPrincipal currentUser);

  /**
   * Clears all messages for the specified room
   *
   * @param roomId room identifier {@link UUID }
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return date since messages were cleared
   */
  OffsetDateTime clearRoomHistory(UUID roomId, UserPrincipal currentUser);

  /**
   * Updates a room information
   *
   * @param roomId room identifier {@link UUID }
   * @param updateRoomRequestDto room fields to update {@link RoomEditableFieldsDto }
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return Updated room {@link RoomDto }
   */
  RoomDto updateRoom(
      UUID roomId, RoomEditableFieldsDto updateRoomRequestDto, UserPrincipal currentUser);

  /**
   * Gets the room picture
   *
   * @param roomId room identifier
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return The room picture
   */
  FileContentAndMetadata getRoomPicture(UUID roomId, UserPrincipal currentUser);

  /**
   * Sets a new room picture
   *
   * @param roomId room identifier {@link UUID }
   * @param imageStream image to set {@link InputStream}
   * @param mimeType image mime type
   * @param contentLength image size
   * @param fileName image file name
   * @param currentUser current authenticated user {@link UserPrincipal}
   */
  void setRoomPicture(
      UUID roomId,
      InputStream imageStream,
      String mimeType,
      Long contentLength,
      String fileName,
      UserPrincipal currentUser);

  /**
   * Deletes the room pictures
   *
   * @param roomId room identifier {@link UUID }
   * @param currentUser current authenticated user {@link UserPrincipal}
   */
  void deleteRoomPicture(UUID roomId, UserPrincipal currentUser);

  /**
   * Updates the workspaces order for the current user
   *
   * @param roomRankDto {@link List} of room identifier and room rank {@link RoomRankDto}
   * @param currentUser current authenticated user {@link UserPrincipal}
   */
  void updateWorkspacesRank(List<RoomRankDto> roomRankDto, UserPrincipal currentUser);

  /**
   * Updates the channels order for the workspace
   *
   * @param workspaceId workspace identifier
   * @param roomRankDto {@link List} of channel identifier and rank {@link RoomRankDto}
   * @param currentUser current authenticated user {@link UserPrincipal}
   */
  void updateChannelsRank(
      UUID workspaceId, List<RoomRankDto> roomRankDto, UserPrincipal currentUser);

  /**
   * Sets the meeting as a reference in the room. This method accepts entities because it's intended
   * to be used only to be called by services.
   *
   * @param room {@link Room} in which to set up the meeting
   * @param meeting {@link Meeting} to set
   */
  void setMeetingIntoRoom(Room room, Meeting meeting);

  /**
   * Forward a list of messages to a specified room
   *
   * @param roomId identifier of the room to send messages to be forwarded
   * @param forwardMessageDto {@link List} of messages to be forwarded {@link ForwardMessageDto}
   * @param currentUser current authenticated user {@link UserPrincipal}
   */
  void forwardMessages(
      UUID roomId, List<ForwardMessageDto> forwardMessageDto, UserPrincipal currentUser);
}
