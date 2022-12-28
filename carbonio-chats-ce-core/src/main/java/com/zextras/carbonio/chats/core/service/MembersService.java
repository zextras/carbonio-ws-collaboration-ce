// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service;

import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.MemberDto;
import com.zextras.carbonio.chats.model.MemberInsertedDto;
import com.zextras.carbonio.chats.model.MemberToInsertDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembersService {

  /**
   * Gets the member data of the requested room by user identifier
   *
   * @param userId user identifier {@link UUID}
   * @param roomId room identifier{@link UUID}
   * @return a {@link MemberDto} wrapped in an {@link Optional}
   */
  Optional<MemberDto> getByUserIdAndRoomId(UUID userId, UUID roomId);

  /**
   * Sets a user as room owner
   *
   * @param roomId      room identifier {@link UUID}
   * @param userId      identifier of the user to set as owner {@link UUID}
   * @param isOwner     if true the user will be set as owner otherwise as a simple member
   * @param currentUser current authenticated user {@link UserPrincipal}
   */
  void setOwner(UUID roomId, UUID userId, boolean isOwner, UserPrincipal currentUser);

  /**
   * Adds the specified user to the room. This can only be performed by an of the given room
   *
   * @param roomId            room identifier {@link UUID }
   * @param memberToInsertDto member to add or invite {@link MemberDto }
   * @param currentUser       current authenticated user {@link UserPrincipal}
   * @return The member added or invited {@link MemberDto }
   **/
  MemberInsertedDto insertRoomMember(UUID roomId, MemberToInsertDto memberToInsertDto, UserPrincipal currentUser);

  /**
   * Removes a member from the specified room. If the specified user is different from the requester, this action is
   * considered as a kick
   *
   * @param roomId      room identifier {@link UUID }
   * @param userId      user identifier {@link UUID }
   * @param currentUser current authenticated user {@link UserPrincipal}
   **/
  void deleteRoomMember(UUID roomId, UUID userId, UserPrincipal currentUser);

  /**
   * Retrieves every member to the given room
   *
   * @param roomId      room identifier {@link UUID }
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return The room members list {@link MemberDto }
   **/
  List<MemberDto> getRoomMembers(UUID roomId, UserPrincipal currentUser);

  List<Subscription> initRoomSubscriptions(List<UUID> membersIds, Room room, UserPrincipal requester);

}
