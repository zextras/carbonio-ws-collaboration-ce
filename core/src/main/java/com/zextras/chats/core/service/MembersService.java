package com.zextras.chats.core.service;

import com.zextras.chats.core.data.entity.Room;
import com.zextras.chats.core.data.entity.Subscription;
import com.zextras.chats.core.model.MemberDto;
import com.zextras.chats.core.web.security.MockUserPrincipal;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.core.SecurityContext;

public interface MembersService {

  void setOwner(Room room, UUID userId, boolean isOwner);

  MemberDto addRoomMember(Room room, UUID userId, boolean asOwner);

  void removeRoomMember(Room room, UUID userId);

  List<MemberDto> getRoomMembers(Room room);

  List<Subscription> initRoomSubscriptions(List<String> membersIds, Room room, MockUserPrincipal requester);

}
