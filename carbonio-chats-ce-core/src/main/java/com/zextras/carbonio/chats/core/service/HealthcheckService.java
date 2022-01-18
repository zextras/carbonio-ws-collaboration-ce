package com.zextras.carbonio.chats.core.service;


import com.zextras.carbonio.chats.model.HealthStatusResponseDto;

public interface HealthcheckService {

  /**
   * Returns whether this service instance is ready to accept requests
   *
   * @return a {@link Boolean} which indicates if this service instance is ready to accept requests or not
   */
  boolean isServiceReady();

  /**
   * Returns an object that represents the service health status
   *
   * @return a {@link HealthStatusResponseDto} which represent this service instance health status
   */
  HealthStatusResponseDto getServiceHealth();
}
