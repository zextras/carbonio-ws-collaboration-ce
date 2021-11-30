package com.zextras.chats.core.web.security;

import com.zextras.chats.core.web.security.model.MockAccount;

public interface AccountService {

  MockAccount getById(String id);
}
