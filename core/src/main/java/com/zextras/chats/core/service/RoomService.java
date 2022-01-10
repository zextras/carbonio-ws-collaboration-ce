package com.zextras.chats.core.service;

import com.zextras.chats.core.data.entity.Room;
import com.zextras.chats.core.model.HashDto;
import com.zextras.chats.core.model.MemberDto;
import com.zextras.chats.core.model.RoomCreationFieldsDto;
import com.zextras.chats.core.model.RoomDto;
import com.zextras.chats.core.model.RoomEditableFieldsDto;
import com.zextras.chats.core.model.RoomInfoDto;
import com.zextras.chats.core.web.security.MockUserPrincipal;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.core.SecurityContext;

public interface RoomService {

  /**
   * get room by identifier and check if the current user is subscribed
   *
   * @param roomId      room identifier {@link UUID}
   * @param currentUser current authenticate user {@link MockUserPrincipal}
   * @param mustBeOwner if true, the user must be a room owner
   * @return The requested room {@link Room}
   */
  Room getRoomAndCheckUser(UUID roomId, MockUserPrincipal currentUser, boolean mustBeOwner);

  /**
   * Creates a room of the specified type
   *
   * @param roomCreationFieldsDto room to create {@link RoomCreationFieldsDto }
   * @param currentUser           current authenticated user {@link MockUserPrincipal}
   * @return The newly created room {@link RoomInfoDto }
   **/
  RoomInfoDto createRoom(RoomCreationFieldsDto roomCreationFieldsDto, MockUserPrincipal currentUser);

  /**
   * Deletes the specified room
   *
   * @param roomId      room identifier {@link UUID }
   * @param currentUser current authenticate user {@link MockUserPrincipal}
   **/
  void deleteRoom(UUID roomId, MockUserPrincipal currentUser);


  /**
   * Retrieves the requested room
   *
   * @param roomId      room identifier {@link UUID }
   * @param currentUser current authenticated user {@link MockUserPrincipal}
   * @return Requested room {@link RoomInfoDto }
   **/
  RoomInfoDto getRoomById(UUID roomId, MockUserPrincipal currentUser);

  /**
   * Retrieves a list of every room the user has access to
   *
   * @param currentUser current authenticated user {@link MockUserPrincipal}
   * @return List of every room that the user has access to {@link RoomDto }
   **/
  List<RoomDto> getRooms(MockUserPrincipal currentUser);

  /**
   * Mutes notification for the specified room
   *
   * @param roomId      room identifier {@link UUID }
   * @param currentUser current authenticated user {@link MockUserPrincipal}
   **/
  void muteRoom(UUID roomId, MockUserPrincipal currentUser);


  /**
   * Resets the specified room hash
   *
   * @param roomId      room identifier {@link UUID }
   * @param currentUser current authenticated user {@link MockUserPrincipal}
   * @return Room's hash {@link HashDto }
   **/
  HashDto resetRoomHash(UUID roomId, MockUserPrincipal currentUser);


  /**
   * Unmutes notification for the specified room
   *
   * @param roomId      room identifier {@link UUID }
   * @param currentUser current authenticated user {@link MockUserPrincipal}
   **/
  void unmuteRoom(UUID roomId, MockUserPrincipal currentUser);

  /**
   * Updates a room information
   *
   * @param roomId                room identifier {@link UUID }
   * @param roomEditableFieldsDto room fields to update {@link RoomEditableFieldsDto }
   * @param currentUser           current authenticated user {@link MockUserPrincipal}
   * @return Updated room {@link RoomDto }
   **/
  RoomDto updateRoom(UUID roomId, RoomEditableFieldsDto roomEditableFieldsDto, MockUserPrincipal currentUser);

}
