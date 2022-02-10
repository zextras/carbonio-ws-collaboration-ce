// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.account.impl;

import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.core.data.model.Account;
import com.zextras.carbonio.usermanagement.UserManagementClient;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;

public class FakeAccountServiceImpl extends AccountServiceImpl {

  private static final List<Account> fakeAccounts = List.of(
    Account.create("332a9527-3388-4207-be77-6d7e2978a723").name("Snoopy").email("snoopy@peanuts.com"),
    Account.create("82735f6d-4c6c-471e-99d9-4eef91b1ec45").name("Charlie Brown").email("charlie.brown@peanuts.com"),
    Account.create("ea7b9b61-bef5-4cf4-80cb-19612c42593a").name("Lucy van Pelt").email("lucy.van.pelt@peanuts.com"),
    Account.create("c91f0b6d-220e-408f-8575-5bf3633fc7f7").name("Linus van Pelt").email("linus.van.pelt@peanuts.com"),
    Account.create("ef196327-acf0-4888-b155-d42dcc659e4d").name("Peperita Patty").email("peperita.patty@peanuts.com"),
    Account.create("120bbfbe-b97b-44d0-81ac-2f23bc244878").name("Marcie Johnson").email("marcie.johnson@peanuts.com"),
    Account.create("92d84bb0-9300-4409-a471-eece9abc614c").name("Schroeder").email("schroeder@peanuts.com"));

  public static List<Account> getFakeAccounts() {
    return fakeAccounts;
  }

  public static String getFakeAuthenticatedUserId() {
    return fakeAccounts.get(0).getId();
  }

  @Inject
  public FakeAccountServiceImpl(UserManagementClient userManagementClient) {
    super(userManagementClient);
  }


  @Override
  public Optional<String> validateToken(String token) {
    return Optional.of(super.validateToken(token).orElse(fakeAccounts.get(0).getId()));
  }

  @Override
  public Optional<Account> getByUUID(UUID userId, UserPrincipal currentUser) {
    return Optional.ofNullable(super.getByUUID(userId, currentUser).orElse(
      fakeAccounts.stream()
        .filter(account -> account.getId().equals(userId.toString()))
        .findAny()
        .orElse(null)));
  }

  @Override
  public boolean isAlive() {
    return super.isAlive();
  }
}
