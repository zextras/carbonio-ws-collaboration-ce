// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
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
   * Adds the specified users to the room. This can only be performed by an owner of the given room
   *
   * @param roomId room identifier {@link UUID }
   * @param memberToInsertDto members to add or invite {@link MemberDto }
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return The member added or invited {@link MemberDto }
   */
  List<MemberInsertedDto> insertRoomMembers(
      UUID roomId, List<MemberToInsertDto> memberToInsertDto, UserPrincipal currentUser);

  /**
   * Removes a member from the specified room. If the specified user is different from the
   * requester, this action is considered as a kick
   *
   * @param roomId room identifier {@link UUID }
   * @param userId user identifier {@link UUID }
   * @param currentUser current authenticated user {@link UserPrincipal}
   */
  void deleteRoomMember(UUID roomId, UUID userId, UserPrincipal currentUser);

  /**
   * Removes a member from the specified room. This is used by ParticipantService to remove a member
   * which has not to be in the room anymore after a meeting has ended
   *
   * @param room room entity {@link Room }
   * @param userId user identifier {@link String }
   */
  void deleteRoomMember(String userId, Room room);

  /**
   * Retrieves every member to the given room
   *
   * @param roomId room identifier {@link UUID }
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return The room members list {@link MemberDto }
   */
  List<MemberDto> getRoomMembers(UUID roomId, UserPrincipal currentUser);

  /**
   * Promotes a member as room owner
   *
   * @param roomId room identifier {@link UUID}
   * @param userId identifier of the member to promote as owner {@link UUID}
   * @param currentUser current authenticated user {@link UserPrincipal}
   */
  void promoteMemberToOwner(UUID roomId, UUID userId, UserPrincipal currentUser);

  /**
   * Demotes an owner to a room member
   *
   * @param roomId room identifier {@link UUID}
   * @param userId identifier of the owner to demote as member {@link UUID}
   * @param currentUser current authenticated user {@link UserPrincipal}
   */
  void demoteOwnerToMember(UUID roomId, UUID userId, UserPrincipal currentUser);

  /**
   * Updates existing room owners. This can only be performed by an owner of the given room
   *
   * @param roomId room identifier {@link UUID }
   * @param members members to update {@link MemberDto }
   * @param currentUser current authenticated user {@link UserPrincipal}
   * @return The member updated {@link MemberDto }
   */
  List<MemberDto> updateRoomOwners(UUID roomId, List<MemberDto> members, UserPrincipal currentUser);

  /**
   * Gets the subscription data of the requested room by user identifier
   *
   * @param userId user identifier {@link UUID}
   * @param roomId room identifier{@link UUID}
   * @return a {@link Subscription} wrapped in an {@link Optional}
   */
  Optional<Subscription> getSubscription(UUID userId, UUID roomId);

  /**
   * Adds every member into the specified room
   *
   * @param members {@link List} of member to add
   * @param room {@link Room} where to add the members to
   * @return {@link List} of {@link Subscription} added
   */
  List<Subscription> initRoomSubscriptions(List<MemberDto> members, Room room);
}
