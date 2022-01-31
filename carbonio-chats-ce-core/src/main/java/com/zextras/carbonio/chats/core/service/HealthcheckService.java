// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service;


import com.zextras.carbonio.chats.model.HealthStatusDto;

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
   * @return a {@link HealthStatusDto} which represent this service instance health status
   */
  HealthStatusDto getServiceHealth();
}
