// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.security;

import com.zextras.carbonio.chats.core.web.security.model.Account;
import java.util.Optional;

public interface AccountService {

  Optional<Account> getById(String id);
}
