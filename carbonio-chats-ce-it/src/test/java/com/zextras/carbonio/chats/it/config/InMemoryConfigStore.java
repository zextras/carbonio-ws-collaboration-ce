package com.zextras.carbonio.chats.it.config;

import com.zextras.carbonio.chats.core.config.ConfigValue;
import java.util.HashMap;
import java.util.Optional;

public class InMemoryConfigStore {

  private static final HashMap<ConfigValue, String> store = new HashMap<>();

  public static Optional<String> get(ConfigValue key) {
    return Optional.ofNullable(store.get(key));
  }

  public static void set(ConfigValue key, String value) {
    store.put(key, value);
  }

}
