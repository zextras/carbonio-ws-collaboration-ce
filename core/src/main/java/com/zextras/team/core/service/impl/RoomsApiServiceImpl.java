package com.zextras.team.core.service.impl;

import com.zextras.team.core.api.RoomsApiService;
import com.zextras.team.core.model.HashDto;
import com.zextras.team.core.model.IdDto;
import com.zextras.team.core.model.MemberDto;
import com.zextras.team.core.model.RoomCreationFieldsDto;
import com.zextras.team.core.model.RoomDto;
import com.zextras.team.core.model.RoomEditableFieldsDto;
import com.zextras.team.core.model.RoomInfoDto;
import java.io.File;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.core.SecurityContext;

public class RoomsApiServiceImpl implements RoomsApiService {

  @Override
  public IdDto addAttachment(UUID roomId, File body, SecurityContext securityContext) {
    return null;
  }

  @Override
  public void addOwner(UUID roomId, UUID userId, SecurityContext securityContext) {

  }

  @Override
  public MemberDto addRoomMember(
    UUID roomId, UUID userid, MemberDto memberDto, SecurityContext securityContext
  ) {
    return null;
  }

  @Override
  public RoomInfoDto createRoom(
    RoomCreationFieldsDto roomCreationFieldsDto, SecurityContext securityContext
  ) {
    return null;
  }

  @Override
  public void deleteRoom(UUID roomId, SecurityContext securityContext) {

  }

  @Override
  public void deleteRoomMember(UUID roomId, UUID userid, SecurityContext securityContext) {

  }

  @Override
  public RoomInfoDto getRoomById(UUID roomId, SecurityContext securityContext) {
    return null;
  }

  @Override
  public List<MemberDto> getRoomMembers(UUID roomId, SecurityContext securityContext) {
    return null;
  }

  @Override
  public List<RoomDto> getRooms(SecurityContext securityContext) {
    return null;
  }

  @Override
  public void muteRoom(UUID roomId, SecurityContext securityContext) {

  }

  @Override
  public void removeOwner(UUID roomId, UUID userId, SecurityContext securityContext) {

  }

  @Override
  public HashDto resetRoomHash(UUID roomId, SecurityContext securityContext) {
    return null;
  }

  @Override
  public void setRoomPicture(UUID roomId, File body, SecurityContext securityContext) {

  }

  @Override
  public void unmuteRoom(UUID roomId, SecurityContext securityContext) {

  }

  @Override
  public RoomDto updateRoom(
    UUID roomId, RoomEditableFieldsDto roomEditableFieldsDto, SecurityContext securityContext
  ) {
    return null;
  }
}
