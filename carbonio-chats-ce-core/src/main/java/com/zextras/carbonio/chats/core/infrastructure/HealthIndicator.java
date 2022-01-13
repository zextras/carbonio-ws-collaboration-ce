package com.zextras.carbonio.chats.core.infrastructure;

public interface HealthIndicator {

  /**
   * Returns whether we can communicate with the component or not
   *
   * @return a {@link Boolean} which indicates if we can communicate with the component or not
   */
  boolean isAlive();
}
