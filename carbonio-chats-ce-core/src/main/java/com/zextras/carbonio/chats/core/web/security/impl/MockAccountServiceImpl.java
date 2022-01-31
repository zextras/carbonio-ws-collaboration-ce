// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.security.impl;

import com.zextras.carbonio.chats.core.web.security.model.Account;
import com.zextras.carbonio.chats.core.web.security.AccountService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MockAccountServiceImpl implements AccountService {

  private static final Map<String, Account> accounts = new HashMap<>();

  static {
    accounts.put("332a9527-3388-4207-be77-6d7e2978a723", new Account("332a9527-3388-4207-be77-6d7e2978a723", "Snoopy"));
    accounts.put("82735f6d-4c6c-471e-99d9-4eef91b1ec45", new Account("82735f6d-4c6c-471e-99d9-4eef91b1ec45", "Charlie Brown"));
    accounts.put("ea7b9b61-bef5-4cf4-80cb-19612c42593a", new Account("ea7b9b61-bef5-4cf4-80cb-19612c42593a", "Lucy van Pelt"));
    accounts.put("c91f0b6d-220e-408f-8575-5bf3633fc7f7", new Account("c91f0b6d-220e-408f-8575-5bf3633fc7f7", "Linus van Pelt"));
    accounts.put("ef196327-acf0-4888-b155-d42dcc659e4d", new Account("ef196327-acf0-4888-b155-d42dcc659e4d", "Peperita Patty"));
    accounts.put("120bbfbe-b97b-44d0-81ac-2f23bc244878", new Account("120bbfbe-b97b-44d0-81ac-2f23bc244878", "Marcie Johnson"));
    accounts.put("92d84bb0-9300-4409-a471-eece9abc614c", new Account("92d84bb0-9300-4409-a471-eece9abc614c", "Schroeder"));
  }

  @Override
  public Optional<Account> getById(String id) {
    return Optional.ofNullable(accounts.get(id));
  }
}
