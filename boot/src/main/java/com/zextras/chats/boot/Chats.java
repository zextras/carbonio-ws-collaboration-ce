package com.zextras.chats.boot;

import com.google.inject.Guice;
import com.zextras.chats.boot.config.BootModule;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

public class Chats {

  public static void main(String[] args) throws Exception {
    Properties properties = new Properties();
    properties.load(Chats.class.getClassLoader().getResourceAsStream("chats.properties"));
    printBanner();
    Guice.createInjector(new BootModule(properties)).getInstance(Boot.class).boot();
  }

  private static void printBanner() {
    try (BufferedReader br = new BufferedReader(
      new InputStreamReader(Objects.requireNonNull(
        Chats.class.getClassLoader().getResourceAsStream("banner.txt"))))) {
      br.lines().collect(Collectors.toList()).forEach(System.out::println);
    } catch (Exception e) {
    }
  }
}
