// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.exceptions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;

@Singleton
@Provider
public class DefaultExceptionHandler extends ExceptionHandler<Exception> {

  @Inject
  public DefaultExceptionHandler() {}

  @Override
  public Response toResponse(Exception exception) {
    return handleException(
        exception,
        exception.getMessage(),
        Status.INTERNAL_SERVER_ERROR.getStatusCode(),
        Status.INTERNAL_SERVER_ERROR.getReasonPhrase(),
        true);
  }
}
