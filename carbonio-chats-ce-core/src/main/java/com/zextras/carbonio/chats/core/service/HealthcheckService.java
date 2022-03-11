// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service;


import com.zextras.carbonio.chats.model.HealthStatusDto;
import com.zextras.carbonio.chats.model.HealthStatusTypeDto;

public interface HealthcheckService {

  /**
   * Returns the service status
   *
   * @return the service status {@link HealthStatusTypeDto}
   */
  HealthStatusTypeDto getServiceStatus();

  /**
   * Returns an object that represents the service health status
   *
   * @return a {@link HealthStatusDto} which represent this service instance health status
   */
  HealthStatusDto getServiceHealth();
}
