// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.boot;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import com.google.inject.Guice;
import com.zextras.carbonio.chats.boot.config.BootModule;
import com.zextras.carbonio.chats.core.config.ChatsConstant;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;

public class Chats {

  public static void main(String[] args) throws Exception {
    if (Files.exists(Path.of(ChatsConstant.LOGGER_CONFIG_PATH))) {
      loadLoggingConfigurations();
    }
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


  private static void loadLoggingConfigurations() {
    System.out.printf("Loading logging configurations from file '%s' ...%n", ChatsConstant.LOGGER_CONFIG_PATH);
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    JoranConfigurator configurator = new JoranConfigurator();
    configurator.setContext(context);
    context.reset();
    try {
      configurator.doConfigure(ChatsConstant.LOGGER_CONFIG_PATH);
      System.out.println("Logging configurations file loaded");
    } catch (JoranException e1) {
      System.out.println("Failed to load logging configurations file");
      e1.printStackTrace();
      try {
        System.out.println("Loading logging console configurations");
        configurator.doConfigure(Chats.class.getClassLoader().getResourceAsStream("logback.xml"));
        System.out.println("Logging console configurations loaded");
      } catch (JoranException e2) {
        System.out.println("Failed to load logging console configurations");
        e2.printStackTrace();
        System.out.println("WARN: No logging was started");
      }
    }
  }
}
