// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.Message;
import com.zextras.carbonio.chats.core.data.entity.SystemEvent;
import com.zextras.carbonio.chats.core.data.model.TimelineItem;
import com.zextras.carbonio.chats.core.data.model.TimelinePage;
import com.zextras.carbonio.chats.core.repository.MessageRepository;
import com.zextras.carbonio.chats.core.repository.SystemEventRepository;
import com.zextras.carbonio.chats.core.repository.TimelineRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class TimelineRepositoryImpl implements TimelineRepository {

  private final MessageRepository messageRepository;
  private final SystemEventRepository systemEventRepository;

  @Inject
  public TimelineRepositoryImpl(
      MessageRepository messageRepository, SystemEventRepository systemEventRepository) {
    this.messageRepository = messageRepository;
    this.systemEventRepository = systemEventRepository;
  }

  @Override
  public TimelinePage findLatest(String roomId, OffsetDateTime after, int limit) {
    int fetch = limit + 1;
    List<TimelineItem> messages =
        wrapMessages(messageRepository.getByRoomId(roomId, null, after, fetch));
    List<TimelineItem> events =
        wrapEvents(systemEventRepository.findByRoomIdBounded(roomId, null, after, fetch));
    return descendingPage(messages, events, limit);
  }

  @Override
  public TimelinePage findBefore(
      String roomId, OffsetDateTime before, String beforeId, OffsetDateTime after, int limit) {
    int fetch = limit + 1;
    List<TimelineItem> messages;
    List<TimelineItem> events;
    if (beforeId != null) {
      messages =
          wrapMessages(messageRepository.getByRoomId(roomId, before, beforeId, after, fetch));
      events =
          wrapEvents(
              systemEventRepository.findByRoomIdComposite(roomId, before, beforeId, after, fetch));
    } else {
      messages = wrapMessages(messageRepository.getByRoomId(roomId, before, after, fetch));
      events = wrapEvents(systemEventRepository.findByRoomIdBounded(roomId, before, after, fetch));
    }
    return descendingPage(messages, events, limit);
  }

  @Override
  public TimelinePage findAfter(String roomId, OffsetDateTime after, int limit) {
    int fetch = limit + 1;
    List<TimelineItem> messages =
        wrapMessages(messageRepository.getByRoomId(roomId, null, after, fetch, true));
    List<TimelineItem> events =
        wrapEvents(systemEventRepository.findByRoomIdForward(roomId, after, null, null, fetch));

    List<TimelineItem> merged =
        TimelineMerger.merge(messages, events, TimelineMerger.ASCENDING, fetch);
    boolean hasMoreAfter = merged.size() > limit;
    List<TimelineItem> page = hasMoreAfter ? new ArrayList<>(merged.subList(0, limit)) : merged;
    return new TimelinePage(page, false, hasMoreAfter);
  }

  @Override
  public TimelinePage findAround(
      String roomId, OffsetDateTime around, OffsetDateTime after, int limit) {
    int half = limit / 2;
    int fetch = half + 1;
    OffsetDateTime olderUpperBound = around.plusNanos(1_000_000);
    OffsetDateTime newerAfter = after != null && after.isAfter(around) ? after : around;

    List<TimelineItem> olderMessages =
        wrapMessages(messageRepository.getByRoomId(roomId, olderUpperBound, after, fetch));
    List<TimelineItem> newerMessages =
        wrapMessages(messageRepository.getByRoomId(roomId, null, newerAfter, fetch, true));
    List<TimelineItem> olderEvents =
        wrapEvents(
            systemEventRepository.findByRoomIdBounded(roomId, olderUpperBound, after, fetch));
    List<TimelineItem> newerEvents =
        wrapEvents(
            systemEventRepository.findByRoomIdForward(roomId, newerAfter, null, null, fetch));

    List<TimelineItem> olderMerged =
        TimelineMerger.merge(olderMessages, olderEvents, TimelineMerger.DESCENDING, fetch);
    List<TimelineItem> newerMerged =
        TimelineMerger.merge(newerMessages, newerEvents, TimelineMerger.ASCENDING, fetch);

    boolean hasMoreBefore = olderMerged.size() > half;
    boolean hasMoreAfter = newerMerged.size() > half;

    List<TimelineItem> older =
        new ArrayList<>(hasMoreBefore ? olderMerged.subList(0, half) : olderMerged);
    older.sort(TimelineMerger.ASCENDING);
    List<TimelineItem> newer = hasMoreAfter ? newerMerged.subList(0, half) : newerMerged;

    List<TimelineItem> items = new ArrayList<>(older);
    items.addAll(newer);
    return new TimelinePage(items, hasMoreBefore, hasMoreAfter);
  }

  private TimelinePage descendingPage(
      List<TimelineItem> messages, List<TimelineItem> events, int limit) {
    List<TimelineItem> merged =
        TimelineMerger.merge(messages, events, TimelineMerger.DESCENDING, limit + 1);
    boolean hasMoreBefore = merged.size() > limit;
    List<TimelineItem> page = new ArrayList<>(hasMoreBefore ? merged.subList(0, limit) : merged);
    page.sort(TimelineMerger.ASCENDING);
    return new TimelinePage(page, hasMoreBefore, false);
  }

  private static List<TimelineItem> wrapMessages(List<Message> messages) {
    return messages.stream()
        .map(message -> (TimelineItem) new TimelineItem.MessageItem(message))
        .toList();
  }

  private static List<TimelineItem> wrapEvents(List<SystemEvent> events) {
    return events.stream()
        .map(event -> (TimelineItem) new TimelineItem.SystemEventItem(event))
        .toList();
  }
}
