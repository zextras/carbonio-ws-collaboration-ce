package com.zextras.carbonio.chats.it.config;

import java.util.HashMap;
import java.util.Optional;

public class InMemoryConfigStore {

  private static final HashMap<String, String> store = new HashMap<>();

  public static Optional<String> get(String key) {
    return Optional.ofNullable(store.get(key));
  }

  public static void set(String key, String value) {
    store.put(key, value);
  }

}
