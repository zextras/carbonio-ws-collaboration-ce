package com.zextras.chats.core.web.dispatcher;

import com.zextras.chats.core.data.event.Event;
import java.util.UUID;

public interface EventDispatcher {

  // TODO: 24/11/21 versioning

  void sentToTopic(UUID sender, UUID topic, Event event);
  void sentToQueue(UUID sender, UUID receiver, Event event);
}
