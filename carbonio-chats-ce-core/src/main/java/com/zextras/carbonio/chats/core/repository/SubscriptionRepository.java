// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.Subscription;
import java.util.Optional;

public interface SubscriptionRepository {

  Optional<Subscription> getById(String roomId, String userId);

  Subscription update(Subscription subscription);

  Subscription insert(Subscription subscription);

  void delete(String roomId, String userId);

}
