package com.zextras.carbonio.chats.core.service;

import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.model.HashDto;
import com.zextras.carbonio.chats.core.model.RoomCreationFieldsDto;
import com.zextras.carbonio.chats.core.model.RoomDto;
import com.zextras.carbonio.chats.core.model.RoomEditableFieldsDto;
import com.zextras.carbonio.chats.core.model.RoomInfoDto;
import com.zextras.carbonio.chats.core.web.security.MockUserPrincipal;
import java.io.File;
import java.util.List;
import java.util.UUID;

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

  /**
   * Gets the room picture
   * @param roomId room identifier
   * @param currentUser current authenticated user {@link MockUserPrincipal}
   * @return The room picture
   */
  FileContentAndMetadata getRoomPicture(UUID roomId, MockUserPrincipal currentUser);

  /**
   * Sets a new room picture
   *
   * @param roomId      room identifier {@link UUID }
   * @param image       image to set {@link File}
   * @param mimeType    image mime type
   * @param fileName    image file name
   * @param currentUser current authenticated user {@link MockUserPrincipal}
   **/
  void setRoomPicture(UUID roomId, File image, String mimeType, String fileName, MockUserPrincipal currentUser);

}
