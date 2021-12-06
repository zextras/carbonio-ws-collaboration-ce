package com.zextras.chats.core.web.security;

import com.zextras.chats.core.web.security.model.Account;
import java.util.Optional;

public interface AccountService {

  Optional<Account> getById(String id);
}
