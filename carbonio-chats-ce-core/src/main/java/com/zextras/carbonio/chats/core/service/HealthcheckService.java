// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service;


import com.zextras.carbonio.chats.model.HealthStatusDto;
import com.zextras.carbonio.chats.model.HealthStatusTypeDto;

public interface HealthcheckService {

  /**
   * Returns a {@link HealthStatusTypeDto} which indicates the service status:
   * <ul>
   *   <li>{@link HealthStatusTypeDto#OK}</li> indicates that everything is working properly
   *   <li>{@link HealthStatusTypeDto#WARN}</li> indicates that a non-fundamental service is not healthy
   *   <li>{@link HealthStatusTypeDto#ERROR}</li> indicates that a fundamental service is not healthy
   * </ul>
   *
   * @return a {@link HealthStatusTypeDto} which indicates the service status
   */
  HealthStatusTypeDto getServiceStatus();

  /**
   * Returns an object that represents the service health status
   *
   * @return a {@link HealthStatusDto} which represent this service instance health status
   */
  HealthStatusDto getServiceHealth();
}
