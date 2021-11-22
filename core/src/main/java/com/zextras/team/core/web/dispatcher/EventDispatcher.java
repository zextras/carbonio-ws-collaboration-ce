package com.zextras.team.core.web.dispatcher;

import com.zextras.team.core.web.dispatcher.model.Event;

public interface EventDispatcher {
  void sentToTopic(Event event);
  void sentToQueue(Event event);
}
