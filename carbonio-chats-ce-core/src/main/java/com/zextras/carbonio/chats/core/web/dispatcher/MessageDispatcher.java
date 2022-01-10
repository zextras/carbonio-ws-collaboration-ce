package com.zextras.carbonio.chats.core.web.dispatcher;

import com.zextras.carbonio.chats.core.web.dispatcher.model.Message;

public interface MessageDispatcher {

  void send(Message message);

}
