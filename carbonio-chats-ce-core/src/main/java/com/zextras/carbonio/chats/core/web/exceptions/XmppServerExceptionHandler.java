// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.exceptions;

import com.google.inject.Singleton;
import com.zextras.carbonio.chats.mongooseim.admin.invoker.ApiException;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

@Provider
@Singleton
public class XmppServerExceptionHandler extends ExceptionHandler<ApiException> {

  @Inject
  public XmppServerExceptionHandler() {
  }

  @Override
  public Response toResponse(ApiException exception) {
    return handleException(exception, exception.getMessage(), exception.getCode(),
      Status.fromStatusCode(exception.getCode()).getReasonPhrase(), true);
  }
}
