package com.zextras.carbonio.chats.core.web.security;

import com.zextras.carbonio.chats.core.web.security.model.Account;
import java.util.Optional;

public interface AccountService {

  Optional<Account> getById(String id);
}
