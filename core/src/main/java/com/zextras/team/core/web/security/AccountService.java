package com.zextras.team.core.web.security;

import com.zextras.team.core.web.security.model.MockAccount;

public interface AccountService {

  MockAccount getById(String id);
}
