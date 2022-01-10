package com.zextras.carbonio.chats.boot;

import com.google.inject.Guice;
import com.zextras.carbonio.chats.boot.config.BootModule;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.stream.Collectors;

public class Chats {

  public static void main(String[] args) throws Exception {
    printBanner();
    Guice.createInjector(new BootModule()).getInstance(Boot.class).boot();
  }

  private static void printBanner() {
    try (BufferedReader br = new BufferedReader(
      new InputStreamReader(Objects.requireNonNull(
        Chats.class.getClassLoader().getResourceAsStream("banner.txt"))))) {
      br.lines().collect(Collectors.toList()).forEach(System.out::println);
    } catch (Exception e) {
      ChatsLogger.error(Chats.class, "Error while printing Chats banner", e);
    }
  }
}
