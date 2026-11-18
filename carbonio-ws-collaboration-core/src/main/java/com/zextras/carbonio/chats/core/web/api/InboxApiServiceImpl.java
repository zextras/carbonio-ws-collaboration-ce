// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.api.InboxApiService;
import com.zextras.carbonio.chats.api.NotFoundException;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Message;
import com.zextras.carbonio.chats.core.data.entity.ReadMarker;
import com.zextras.carbonio.chats.core.data.model.InboxLastItem;
import com.zextras.carbonio.chats.core.data.model.PresenceSummary;
import com.zextras.carbonio.chats.core.data.model.TimelineItem;
import com.zextras.carbonio.chats.core.data.model.VisibilityFloor;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.infrastructure.event.mapper.TimelineItemMapper;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.ReadMarkerRepository;
import com.zextras.carbonio.chats.core.service.ChatMessageService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.service.presence.PresenceService;
import com.zextras.carbonio.chats.core.web.api.mapper.RoomMapper;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.InboxEntryDto;
import com.zextras.carbonio.chats.model.InboxMemberDto;
import com.zextras.carbonio.chats.model.InboxResponseDto;
import com.zextras.carbonio.chats.model.InboxRoomDto;
import com.zextras.carbonio.chats.model.MemberDto;
import com.zextras.carbonio.chats.model.ReadMarkerDto;
import com.zextras.carbonio.chats.model.RoomDto;
import com.zextras.carbonio.chats.model.RoomExtraFieldDto;
import io.ebean.annotation.Transactional;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class InboxApiServiceImpl implements InboxApiService {

  private final ChatMessageService chatMessageService;
  private final RoomService roomService;
  private final FileMetadataRepository fileMetadataRepository;
  private final ReadMarkerRepository readMarkerRepository;
  private final PresenceService presenceService;
  private final TimelineItemMapper timelineItemMapper;
  private final RoomMapper roomMapper;

  @Inject
  public InboxApiServiceImpl(
      ChatMessageService chatMessageService,
      RoomService roomService,
      FileMetadataRepository fileMetadataRepository,
      ReadMarkerRepository readMarkerRepository,
      PresenceService presenceService,
      TimelineItemMapper timelineItemMapper,
      RoomMapper roomMapper) {
    this.chatMessageService = chatMessageService;
    this.roomService = roomService;
    this.fileMetadataRepository = fileMetadataRepository;
    this.readMarkerRepository = readMarkerRepository;
    this.presenceService = presenceService;
    this.timelineItemMapper = timelineItemMapper;
    this.roomMapper = roomMapper;
  }

  private static UserPrincipal getCurrentUser(SecurityContext securityContext) {
    return Optional.ofNullable((UserPrincipal) securityContext.getUserPrincipal())
        .orElseThrow(UnauthorizedException::new);
  }

  @Override
  @Transactional(readOnly = true)
  public Response getInbox(SecurityContext securityContext) throws NotFoundException {
    UserPrincipal currentUser = getCurrentUser(securityContext);

    // 1. All rooms with subscriptions (1 query)
    List<RoomDto> rooms =
        roomMapper.ent2dto(
            roomService.getRooms(
                List.of(RoomExtraFieldDto.MEMBERS, RoomExtraFieldDto.SETTINGS), currentUser),
            roomService.getRoomsUserSettings(currentUser),
            true,
            true);
    List<String> roomIds = rooms.stream().map(r -> r.getId().toString()).toList();

    // 2. Last item + last message per room (independent of room size)
    Map<String, InboxLastItem> lastItemByRoom =
        chatMessageService.getInboxLastItems(currentUser.getId());

    // 3. All read markers for all rooms (1 query) — needed for blue-tick computation on sender side
    Map<String, List<ReadMarker>> allMarkersByRoom = new HashMap<>();
    if (!roomIds.isEmpty()) {
      for (ReadMarker mr : readMarkerRepository.getByRoomIds(roomIds)) {
        allMarkersByRoom.computeIfAbsent(mr.getId().getRoomId(), k -> new ArrayList<>()).add(mr);
      }
    }

    // 4. Capped unread counts per room (1 query)
    Map<String, Long> unreadCountByRoom = chatMessageService.getUnreadCounts(currentUser.getId());

    // 5. Bulk-load file metadata for the attachments of the last items (1 query)
    List<String> attachmentIds =
        lastItemByRoom.values().stream()
            .flatMap(
                i ->
                    Stream.of(
                        i.lastItem() instanceof TimelineItem.MessageItem m ? m.message() : null,
                        i.repliedMessage()))
            .filter(Objects::nonNull)
            .map(Message::getAttachmentId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    Map<String, FileMetadata> metadataByFileId =
        attachmentIds.isEmpty()
            ? Map.of()
            : fileMetadataRepository.getByIds(attachmentIds).stream()
                .collect(Collectors.toMap(FileMetadata::getId, f -> f, (a, b) -> a));

    // 6. Batch fetch presence for all members (1 SQL query)
    List<String> allMemberIds =
        rooms.stream()
            .filter(r -> r.getMembers() != null)
            .flatMap(r -> r.getMembers().stream())
            .map(m -> m.getUserId().toString())
            .distinct()
            .toList();
    Map<String, PresenceSummary> presenceById =
        allMemberIds.isEmpty()
            ? Map.of()
            : presenceService.getBatchPresence(allMemberIds).stream()
                .collect(Collectors.toMap(PresenceSummary::userId, p -> p, (a, b) -> a));

    List<InboxEntryDto> conversations = new ArrayList<>();
    for (RoomDto room : rooms) {
      String roomId = room.getId().toString();
      InboxLastItem lastItem = lastItemByRoom.get(roomId);

      boolean muted =
          room.getUserSettings() != null && Boolean.TRUE.equals(room.getUserSettings().isMuted());

      List<InboxMemberDto> enrichedMembers = new ArrayList<>();
      if (room.getMembers() != null) {
        for (MemberDto member : room.getMembers()) {
          String memberId = member.getUserId().toString();
          InboxMemberDto enriched =
              InboxMemberDto.create()
                  .userId(member.getUserId())
                  .owner(Boolean.TRUE.equals(member.isOwner()));
          PresenceSummary presence = presenceById.get(memberId);
          if (presence != null) {
            enriched.online(presence.online());
            if (presence.lastSeen() != null) {
              enriched.lastActivity(presence.lastSeen().withOffsetSameInstant(ZoneOffset.UTC));
            }
          } else {
            enriched.online(false);
          }
          enrichedMembers.add(enriched);
        }
      }

      InboxEntryDto conversation =
          InboxEntryDto.create()
              .roomId(room.getId())
              .room(
                  InboxRoomDto.create()
                      .id(room.getId())
                      .name(room.getName())
                      .description(room.getDescription())
                      .type(room.getType())
                      .members(enrichedMembers)
                      .userSettings(room.getUserSettings())
                      .meetingId(room.getMeetingId())
                      .pictureUpdatedAt(room.getPictureUpdatedAt())
                      .createdAt(room.getCreatedAt())
                      .updatedAt(room.getUpdatedAt()))
              .unreadCount(unreadCountByRoom.getOrDefault(roomId, 0L).intValue())
              .muted(muted);
      if (lastItem != null) {
        conversation.lastItem(
            timelineItemMapper.toTimelineItem(
                lastItem.lastItem(),
                metadataByFileId,
                lastItem.repliedMessage() == null
                    ? Map.of()
                    : Map.of(lastItem.repliedMessage().getId(), lastItem.repliedMessage())));
      }

      VisibilityFloor floor =
          new VisibilityFloor(
              room.getUserSettings() == null ? null : room.getUserSettings().getClearedAt());
      List<ReadMarkerDto> markers = new ArrayList<>();
      for (ReadMarker marker : allMarkersByRoom.getOrDefault(roomId, List.of())) {
        markers.add(
            ReadMarkerDto.create()
                .userId(UUID.fromString(marker.getUserId()))
                .itemCreatedAt(floor.clamp(marker.getItemCreatedAt())));
      }
      conversation.markers(markers);
      conversations.add(conversation);
    }

    return Response.status(Status.OK)
        .entity(InboxResponseDto.create().conversations(conversations))
        .build();
  }
}
