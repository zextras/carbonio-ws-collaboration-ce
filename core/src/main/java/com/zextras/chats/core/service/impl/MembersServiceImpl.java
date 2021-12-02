package com.zextras.chats.core.service.impl;

import com.zextras.chats.core.data.entity.Room;
import com.zextras.chats.core.data.entity.Subscription;
import com.zextras.chats.core.data.event.RoomOwnerChangedEvent;
import com.zextras.chats.core.exception.ForbiddenException;
import com.zextras.chats.core.repository.SubscriptionRepository;
import com.zextras.chats.core.service.MembersService;
import com.zextras.chats.core.web.dispatcher.EventDispatcher;
import com.zextras.chats.core.web.security.MockSecurityContext;
import com.zextras.chats.core.web.security.MockUserPrincipal;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.inject.Inject;

public class MembersServiceImpl implements MembersService {

  private final SubscriptionRepository subscriptionRepository;
  private final EventDispatcher        eventDispatcher;
  private final MockSecurityContext    mockSecurityContext;

  @Inject
  public MembersServiceImpl(
    SubscriptionRepository subscriptionRepository, EventDispatcher eventDispatcher, MockSecurityContext mockSecurityContext
  ) {
    this.subscriptionRepository = subscriptionRepository;
    this.eventDispatcher = eventDispatcher;
    this.mockSecurityContext = mockSecurityContext;
  }

  @Override
  public void setOwner(Room room, UUID userId, boolean isOwner) {
    // find member
    Subscription subscription = room.getSubscriptions().stream()
      .filter(s -> s.getUserId().equals(userId.toString()))
      .findAny()
      .orElseThrow(() -> new ForbiddenException(String.format("User '%s' is not a member of the room", userId.toString())));
    // change owner value
    subscription.owner(isOwner);
    // update row
    subscriptionRepository.update(subscription);
    // send event at all room members
    eventDispatcher.sentToTopic(
      ((MockUserPrincipal) mockSecurityContext.getUserPrincipal().get()).getId(), UUID.fromString(room.getId()),
      RoomOwnerChangedEvent.create(userId, OffsetDateTime.now())
        .memberModifiedId(userId).isOwner(false)
    );
  }
}
