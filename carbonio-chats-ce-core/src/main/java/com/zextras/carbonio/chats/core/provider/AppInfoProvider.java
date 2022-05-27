package com.zextras.carbonio.chats.core.provider;

import java.util.Optional;

public interface AppInfoProvider {

  /**
   * Retrieves the current Chats chats-information
   *
   * @return the current chats chats-information
   */
  Optional<String> getVersion();
}
