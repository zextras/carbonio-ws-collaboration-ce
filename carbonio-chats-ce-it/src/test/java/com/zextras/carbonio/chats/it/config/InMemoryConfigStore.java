package com.zextras.carbonio.chats.it.config;

import com.zextras.carbonio.chats.core.config.ConfigName;
import java.util.HashMap;
import java.util.Optional;

public class InMemoryConfigStore {

  private static final HashMap<ConfigName, String> store = new HashMap<>();

  public static Optional<String> get(ConfigName key) {
    return Optional.ofNullable(store.get(key));
  }

  public static void set(ConfigName key, String value) {
    store.put(key, value);
  }

}
