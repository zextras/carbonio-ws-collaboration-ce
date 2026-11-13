// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config;

import com.google.inject.ImplementedBy;
import com.zextras.carbonio.chats.core.config.impl.AppConfigCredentialReader;
import java.util.Optional;

@ImplementedBy(AppConfigCredentialReader.class)
public interface MessageDispatcherCredentialReader {

  Optional<String> read(ConfigName name);
}
