package com.zextras.team.core.web.dispatcher;

import com.zextras.team.core.web.dispatcher.model.Message;

public interface MessageDispatcher {

  void send(Message message);

}
