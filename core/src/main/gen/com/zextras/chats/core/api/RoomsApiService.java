package com.zextras.chats.core.api;


import java.io.File;
import com.zextras.chats.core.model.HashDto;
import com.zextras.chats.core.model.IdDto;
import com.zextras.chats.core.model.MemberDto;
import com.zextras.chats.core.model.RoomCreationFieldsDto;
import com.zextras.chats.core.model.RoomDto;
import com.zextras.chats.core.model.RoomEditableFieldsDto;
import com.zextras.chats.core.model.RoomInfoDto;
import java.util.UUID;
import java.util.List;
import javax.annotation.Generated;
import javax.ws.rs.core.SecurityContext;

@Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen")
public interface RoomsApiService {

  /**
   * Uploads a new attachment
   *
   * @param roomId room identifier {@link UUID }
   * @param body file stream 
   * @param securityContext security context {@link SecurityContext}
   * @return File identifier {@link IdDto }
  **/
  IdDto addAttachment(UUID roomId, File body, SecurityContext securityContext);

  /**
   * Promotes a member to owner
   *
   * @param roomId room identifier {@link UUID }
   * @param userId user identifier {@link UUID }
   * @param securityContext security context {@link SecurityContext}
  **/
  void addOwner(UUID roomId, UUID userId, SecurityContext securityContext);

  /**
   * Adds the specified user to the room. This can only be performed by an of the given room 
   *
   * @param roomId room identifier {@link UUID }
   * @param memberDto member to add or invite {@link MemberDto }
   * @param securityContext security context {@link SecurityContext}
   * @return The member added or invited {@link MemberDto }
  **/
  MemberDto addRoomMember(UUID roomId, MemberDto memberDto, SecurityContext securityContext);

  /**
   * Creates a room of the specified type
   *
   * @param roomCreationFieldsDto room to create {@link RoomCreationFieldsDto }
   * @param securityContext security context {@link SecurityContext}
   * @return The newly created room {@link RoomInfoDto }
  **/
  RoomInfoDto createRoom(RoomCreationFieldsDto roomCreationFieldsDto, SecurityContext securityContext);

  /**
   * Deletes the specified room
   *
   * @param roomId room identifier {@link UUID }
   * @param securityContext security context {@link SecurityContext}
  **/
  void deleteRoom(UUID roomId, SecurityContext securityContext);

  /**
   * Retrieves the requested room
   *
   * @param roomId room identifier {@link UUID }
   * @param securityContext security context {@link SecurityContext}
   * @return Requested room {@link RoomInfoDto }
  **/
  RoomInfoDto getRoomById(UUID roomId, SecurityContext securityContext);

  /**
   * Retrieves every member to the given room
   *
   * @param roomId room identifier {@link UUID }
   * @param securityContext security context {@link SecurityContext}
   * @return The room members list {@link MemberDto }
  **/
  List<MemberDto> getRoomMembers(UUID roomId, SecurityContext securityContext);

  /**
   * Retrieves a list of every room the user has access to
   *
   * @param securityContext security context {@link SecurityContext}
   * @return List of every room that the user has access to {@link RoomDto }
  **/
  List<RoomDto> getRooms(SecurityContext securityContext);

  /**
   * Mutes notification for the specified room
   *
   * @param roomId room identifier {@link UUID }
   * @param securityContext security context {@link SecurityContext}
  **/
  void muteRoom(UUID roomId, SecurityContext securityContext);

  /**
   * Demotes a member from owner to normal member
   *
   * @param roomId room identifier {@link UUID }
   * @param userId user identifier {@link UUID }
   * @param securityContext security context {@link SecurityContext}
  **/
  void removeOwner(UUID roomId, UUID userId, SecurityContext securityContext);

  /**
   * Removes a member from the specified room. If the specified user is different from the requester, this action is considered as a kick 
   *
   * @param roomId room identifier {@link UUID }
   * @param userId user identifier {@link UUID }
   * @param securityContext security context {@link SecurityContext}
  **/
  void removeRoomMember(UUID roomId, UUID userId, SecurityContext securityContext);

  /**
   * Resets the specified room hash
   *
   * @param roomId room identifier {@link UUID }
   * @param securityContext security context {@link SecurityContext}
   * @return Room's hash {@link HashDto }
  **/
  HashDto resetRoomHash(UUID roomId, SecurityContext securityContext);

  /**
   * Uploads and sets a new room picture
   *
   * @param roomId room identifier {@link UUID }
   * @param body image to set 
   * @param securityContext security context {@link SecurityContext}
  **/
  void setRoomPicture(UUID roomId, File body, SecurityContext securityContext);

  /**
   * Unmutes notification for the specified room
   *
   * @param roomId room identifier {@link UUID }
   * @param securityContext security context {@link SecurityContext}
  **/
  void unmuteRoom(UUID roomId, SecurityContext securityContext);

  /**
   * Updates a room information
   *
   * @param roomId room identifier {@link UUID }
   * @param roomEditableFieldsDto room fields to update {@link RoomEditableFieldsDto }
   * @param securityContext security context {@link SecurityContext}
   * @return Updated room {@link RoomDto }
  **/
  RoomDto updateRoom(UUID roomId, RoomEditableFieldsDto roomEditableFieldsDto, SecurityContext securityContext);

}
