package com.zextras.carbonio.chats.core.service;

import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.model.MemberDto;
import com.zextras.carbonio.chats.core.web.security.MockUserPrincipal;
import java.util.List;
import java.util.UUID;

public interface MembersService {

  /**
   * Sets a user as room owner
   *
   * @param roomId      room identifier {@link UUID}
   * @param userId      identifier of the user to set as owner {@link UUID}
   * @param isOwner     if true the user will be set as owner otherwise as a simple member
   * @param currentUser current authenticated user {@link MockUserPrincipal}
   */
  void setOwner(UUID roomId, UUID userId, boolean isOwner, MockUserPrincipal currentUser);

  /**
   * Adds the specified user to the room. This can only be performed by an of the given room
   *
   * @param roomId      room identifier {@link UUID }
   * @param memberDto   member to add or invite {@link MemberDto }
   * @param currentUser current authenticated user {@link MockUserPrincipal}
   * @return The member added or invited {@link MemberDto }
   **/
  MemberDto addRoomMember(UUID roomId, MemberDto memberDto, MockUserPrincipal currentUser);

  /**
   * Removes a member from the specified room. If the specified user is different from the requester, this action is considered as a kick
   *
   * @param roomId          room identifier {@link UUID }
   * @param userId          user identifier {@link UUID }
   * @param currentUser     current authenticated user {@link MockUserPrincipal}
   **/
  void removeRoomMember(UUID roomId, UUID userId, MockUserPrincipal currentUser);


  /**
   * Retrieves every member to the given room
   *
   * @param roomId      room identifier {@link UUID }
   * @param currentUser current authenticated user {@link MockUserPrincipal}
   * @return The room members list {@link MemberDto }
   **/
  List<MemberDto> getRoomMembers(UUID roomId, MockUserPrincipal currentUser);

  List<Subscription> initRoomSubscriptions(List<String> membersIds, Room room, MockUserPrincipal requester);

}
