// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for wrapping the methods of the logger
 */
public class ChatsLogger {

  /**
   * Enumerator to define del logging states
   */
  private enum ChatsLoggerLevel {
    ERROR, WARN, INFO, DEBUG, TRACE
  }

  /**
   * Unique method to wrap slf4j logger
   *
   * @param state     log state {@link ChatsLoggerLevel}
   * @param clazz     class from which the log comes
   * @param message   message to log
   * @param throwable exception to log
   */
  private static void log(ChatsLoggerLevel state, Class<?> clazz, String message, Throwable throwable) {
    Logger logger = LoggerFactory.getLogger(clazz == null ? ChatsLogger.class : clazz); //TODO check class
    switch (state) {
      case ERROR:
        logger.error(message, throwable);
        break;
      case WARN:
        logger.warn(message, throwable);
        break;
      case INFO:
        logger.info(message, throwable);
        break;
      case DEBUG:
        logger.debug(message, throwable);
        break;
      case TRACE:
        logger.trace(message, throwable);
        break;
    }
  }

  //TODO context info

  /**
   * Generic log the error message
   *
   * @param message message to log
   */
  public static void error(String message) {
    error(null, message, null);
  }

  /**
   * Log error message from a specific class
   *
   * @param clazz   class from which the log comes
   * @param message message to log
   */
  public static void error(Class<?> clazz, String message) {
    error(clazz, message, null);
  }

  /**
   * Generic log error message and exception and exception
   *
   * @param message   message to log
   * @param throwable exception to log
   */
  public static void error(String message, Throwable throwable) {
    error(null, message, throwable);
  }

  /**
   * Log error message and exception from a specific class
   *
   * @param clazz     class from which the log comes
   * @param message   message to log
   * @param throwable exception to log
   */
  public static void error(Class<?> clazz, String message, Throwable throwable) {
    log(ChatsLoggerLevel.ERROR, clazz, message, throwable);
  }

  /**
   * Generic log warn message
   *
   * @param message message to log
   */
  public static void warn(String message) {
    warn(null, message, null);
  }

  /**
   * Log warn message from a specific class
   *
   * @param clazz   class from which the log comes
   * @param message message to log
   */
  public static void warn(Class<?> clazz, String message) {
    warn(clazz, message, null);
  }

  /**
   * Generic log warn message and exception
   *
   * @param message   message to log
   * @param throwable exception to log
   */
  public static void warn(String message, Throwable throwable) {
    warn(null, message, throwable);
  }

  /**
   * Log warn message and exception from a specific class
   *
   * @param clazz     class from which the log comes
   * @param message   message to log
   * @param throwable exception to log
   */
  public static void warn(Class<?> clazz, String message, Throwable throwable) {
    log(ChatsLoggerLevel.WARN, clazz, message, throwable);
  }

  /**
   * Generic log info message
   *
   * @param message message to log
   */
  public static void info(String message) {
    info(null, message, null);
  }

  /**
   * Log info message from a specific class
   *
   * @param clazz   class from which the log comes
   * @param message message to log
   */
  public static void info(Class<?> clazz, String message) {
    info(clazz, message, null);
  }

  /**
   * Generic log info message and exception
   *
   * @param message   message to log
   * @param throwable exception to log
   */
  public static void info(String message, Throwable throwable) {
    info(null, message, throwable);
  }

  /**
   * Log info message and exception from a specific class
   *
   * @param clazz     class from which the log comes
   * @param message   message to log
   * @param throwable exception to log
   */
  public static void info(Class<?> clazz, String message, Throwable throwable) {
    log(ChatsLoggerLevel.INFO, clazz, message, throwable);
  }

  /**
   * Generic log debug message
   *
   * @param message message to log
   */
  public static void debug(String message) {
    debug(null, message, null);
  }

  /**
   * Log debug message from a specific class
   *
   * @param clazz   class from which the log comes
   * @param message message to log
   */
  public static void debug(Class<?> clazz, String message) {
    debug(clazz, message, null);
  }

  /**
   * Generic log debug message and exception
   *
   * @param message   message to log
   * @param throwable exception to log
   */
  public static void debug(String message, Throwable throwable) {
    debug(null, message, throwable);
  }

  /**
   * Log debug message and exception from a specific class
   *
   * @param clazz     class from which the log comes
   * @param message   message to log
   * @param throwable exception to log
   */
  public static void debug(Class<?> clazz, String message, Throwable throwable) {
    log(ChatsLoggerLevel.DEBUG, clazz, message, throwable);
  }

  /**
   * Generic log trace message
   *
   * @param message message to log
   */
  public static void trace(String message) {
    trace(null, message, null);
  }

  /**
   * Log trace message from a specific class
   *
   * @param clazz   class from which the log comes
   * @param message message to log
   */
  public static void trace(Class<?> clazz, String message) {
    trace(clazz, message, null);
  }

  /**
   * Generic log trace message and exception
   *
   * @param message   message to log
   * @param throwable exception to log
   */
  public static void trace(String message, Throwable throwable) {
    trace(null, message, throwable);
  }

  /**
   * Log trace message and exception from a specific class
   *
   * @param clazz     class from which the log comes
   * @param message   message to log
   * @param throwable exception to log
   */
  public static void trace(Class<?> clazz, String message, Throwable throwable) {
    log(ChatsLoggerLevel.TRACE, clazz, message, throwable);
  }
}
