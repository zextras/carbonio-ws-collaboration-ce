package com.zextras.chats.core.web.security.impl;

import com.zextras.chats.core.web.security.model.MockAccount;
import com.zextras.chats.core.web.security.AccountService;
import java.util.HashMap;
import java.util.Map;

public class MockAccountServiceImpl implements AccountService {

  private static final Map<String, MockAccount> accounts = new HashMap<>();

  static {
    accounts.put("332a9527-3388-4207-be77-6d7e2978a723", new MockAccount("332a9527-3388-4207-be77-6d7e2978a723", "Snoopy"));
    accounts.put("82735f6d-4c6c-471e-99d9-4eef91b1ec45", new MockAccount("82735f6d-4c6c-471e-99d9-4eef91b1ec45", "Charlie Brown"));
    accounts.put("ea7b9b61-bef5-4cf4-80cb-19612c42593a", new MockAccount("ea7b9b61-bef5-4cf4-80cb-19612c42593a", "Lucy van Pelt"));
    accounts.put("c91f0b6d-220e-408f-8575-5bf3633fc7f7", new MockAccount("c91f0b6d-220e-408f-8575-5bf3633fc7f7", "Linus van Pelt"));
    accounts.put("ef196327-acf0-4888-b155-d42dcc659e4d", new MockAccount("ef196327-acf0-4888-b155-d42dcc659e4d", "Peperita Patty"));
    accounts.put("120bbfbe-b97b-44d0-81ac-2f23bc244878", new MockAccount("120bbfbe-b97b-44d0-81ac-2f23bc244878", "Marcie Johnson"));
    accounts.put("92d84bb0-9300-4409-a471-eece9abc614c", new MockAccount("92d84bb0-9300-4409-a471-eece9abc614c", "Schroeder"));
  }

  @Override
  public MockAccount getById(String id) {
    return accounts.get(id);
  }
}
