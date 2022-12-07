package com.zextras.carbonio.chats.core.infrastructure;

import com.zextras.carbonio.chats.model.DependencyHealthTypeDto;

public enum DependencyType {
  DATABASE(DependencyHealthTypeDto.DATABASE.toString(), true),
  AUTHENTICATION_SERVICE(DependencyHealthTypeDto.AUTHENTICATION_SERVICE.toString(), true),
  PROFILING_SERVICE(DependencyHealthTypeDto.PROFILING_SERVICE.toString(), true),
  XMPP_SERVER(DependencyHealthTypeDto.XMPP_SERVER.toString(), true),
  EVENT_DISPATCHER(DependencyHealthTypeDto.EVENT_DISPATCHER.toString(), false),
  STORAGE_SERVICE(DependencyHealthTypeDto.STORAGE_SERVICE.toString(), false),
  PREVIEWER_SERVICE(DependencyHealthTypeDto.PREVIEWER_SERVICE.toString(), false),
  VIDEOSERVER_SERVICE(DependencyHealthTypeDto.VIDEOSERVER_SERVICE.toString(), false);

  private final String  name;
  private final boolean required;

  DependencyType(String name, boolean required) {
    this.name = name;
    this.required = required;
  }

  public String getName() {
    return name;
  }

  public boolean isRequired() {
    return required;
  }
}
