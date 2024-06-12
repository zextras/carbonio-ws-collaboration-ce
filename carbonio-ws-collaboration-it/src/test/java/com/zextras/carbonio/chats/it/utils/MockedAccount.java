// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.utils;

import com.zextras.carbonio.chats.core.data.model.UserProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MockedAccount {

  public enum MockedAccountType {
    SNOOPY,
    CHARLIE_BROWN,
    LUCY_VAN_PELT,
    LINUS_VAN_PELT,
    PEPERITA_PATTY,
    MARCIE_JOHNSON,
    SCHROEDER
  }

  private static final Map<MockedAccountType, MockUserProfile> mockedAccountsMap =
      Map.of(
          MockedAccountType.SNOOPY,
          MockUserProfile.create("332a9527-3388-4207-be77-6d7e2978a723")
              .name("Snoopy")
              .email("snoopy@peanuts.com")
              .token("6g2R31FDn9epUpbyLhZSltqACqd33K9qa0b3lsJL"),
          MockedAccountType.CHARLIE_BROWN,
          MockUserProfile.create("82735f6d-4c6c-471e-99d9-4eef91b1ec45")
              .name("Charlie Brown")
              .email("charlie.brown@peanuts.com")
              .token("jQahMjjCaNOWTLZEvsec7ObuioEeeb4bKOzybjAd"),
          MockedAccountType.LUCY_VAN_PELT,
          MockUserProfile.create("ea7b9b61-bef5-4cf4-80cb-19612c42593a")
              .name("Lucy van Pelt")
              .email("lucy.van.pelt@peanuts.com")
              .token("SuhM5XvKfc6Ex8w1VMeRkuEjuqXXXes0td5s3ce9"),
          MockedAccountType.LINUS_VAN_PELT,
          MockUserProfile.create("c91f0b6d-220e-408f-8575-5bf3633fc7f7")
              .name("Linus van Pelt")
              .email("linus.van.pelt@peanuts.com")
              .token("Cj5x9bgeU4SvvfoiA7zYJnYCH1hQHCgtRJY7MCIH"),
          MockedAccountType.PEPERITA_PATTY,
          MockUserProfile.create("ef196327-acf0-4888-b155-d42dcc659e4d")
              .name("Peperita Patty")
              .email("peperita.patty@peanuts.com")
              .token("EHh8T78Lr40Sr4D5ENGXvUunHmgdi8e9xVkBMMCy"),
          MockedAccountType.MARCIE_JOHNSON,
          MockUserProfile.create("120bbfbe-b97b-44d0-81ac-2f23bc244878")
              .name("Marcie Johnson")
              .email("marcie.johnson@peanuts.com")
              .token("LkyrcPXr7JuZ1eUluFCYnvgmuQEDdOJNO6FaMdH9"),
          MockedAccountType.SCHROEDER,
          MockUserProfile.create("92d84bb0-9300-4409-a471-eece9abc614c")
              .name("Schroeder")
              .email("schroeder@peanuts.com")
              .token("F2TkzabOK2pu91sL951ofbJ7Ur3zcJKV9gBwdB84"));

  public static List<MockUserProfile> getAccounts() {
    return new ArrayList<>(mockedAccountsMap.values());
  }

  public static MockUserProfile getAccount(MockedAccountType type) {
    return mockedAccountsMap.get(type);
  }

  public static class MockUserProfile extends UserProfile {

    private String token;

    public MockUserProfile(String id) {
      super(id);
    }

    public static MockUserProfile create(String id) {
      return new MockUserProfile(id);
    }

    public UUID getUUID() {
      return UUID.fromString(super.getId());
    }

    public MockUserProfile id(String id) {
      super.id(id);
      return this;
    }

    public MockUserProfile email(String email) {
      super.email(email);
      return this;
    }

    public MockUserProfile name(String name) {
      super.name(name);
      return this;
    }

    public MockUserProfile domain(String domain) {
      super.domain(domain);
      return this;
    }

    public String getToken() {
      return token;
    }

    public MockUserProfile token(String token) {
      this.token = token;
      return this;
    }
  }
}
