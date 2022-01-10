package com.zextras.chats.core.api;

import com.zextras.chats.core.api.*;
import com.zextras.chats.core.model.*;


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
import com.zextras.chats.core.api.NotFoundException;

import java.io.InputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen")
public interface RoomsApiService {
      Response addAttachment(UUID roomId,File body,SecurityContext securityContext)
      throws NotFoundException;
      Response addOwner(UUID roomId,UUID userId,SecurityContext securityContext)
      throws NotFoundException;
      Response addRoomMember(UUID roomId,MemberDto memberDto,SecurityContext securityContext)
      throws NotFoundException;
      Response createRoom(RoomCreationFieldsDto roomCreationFieldsDto,SecurityContext securityContext)
      throws NotFoundException;
      Response deleteRoom(UUID roomId,SecurityContext securityContext)
      throws NotFoundException;
      Response getRoomById(UUID roomId,SecurityContext securityContext)
      throws NotFoundException;
      Response getRoomMembers(UUID roomId,SecurityContext securityContext)
      throws NotFoundException;
      Response getRooms(SecurityContext securityContext)
      throws NotFoundException;
      Response muteRoom(UUID roomId,SecurityContext securityContext)
      throws NotFoundException;
      Response removeOwner(UUID roomId,UUID userId,SecurityContext securityContext)
      throws NotFoundException;
      Response removeRoomMember(UUID roomId,UUID userId,SecurityContext securityContext)
      throws NotFoundException;
      Response resetRoomHash(UUID roomId,SecurityContext securityContext)
      throws NotFoundException;
      Response setRoomPicture(UUID roomId,File body,SecurityContext securityContext)
      throws NotFoundException;
      Response unmuteRoom(UUID roomId,SecurityContext securityContext)
      throws NotFoundException;
      Response updateRoom(UUID roomId,RoomEditableFieldsDto roomEditableFieldsDto,SecurityContext securityContext)
      throws NotFoundException;
}
