// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.socket;

import com.zextras.carbonio.ws.core.security.SecurityAttributes;
import io.quarkus.websockets.next.HttpUpgradeCheck;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/** Same rules as REST: anonymous gives 401, inactive or WSC disabled gives 403. */
@ApplicationScoped
public class WscUpgradeCheck implements HttpUpgradeCheck {

  @Override
  public Uni<CheckResult> perform(HttpUpgradeContext context) {
    return context
        .securityIdentity()
        .map(
            identity -> {
              if (identity == null || identity.isAnonymous()) {
                return CheckResult.rejectUpgradeSync(401);
              }
              if (identity.getAttribute(SecurityAttributes.DENIED_REASON) != null) {
                return CheckResult.rejectUpgradeSync(403);
              }
              return CheckResult.permitUpgradeSync();
            });
  }
}
