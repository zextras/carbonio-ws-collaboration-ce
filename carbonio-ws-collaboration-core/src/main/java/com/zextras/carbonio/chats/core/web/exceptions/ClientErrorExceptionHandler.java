// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.exceptions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;

@Singleton
@Provider
public class ClientErrorExceptionHandler extends ExceptionHandler<ClientErrorException> {

  @Inject
  public ClientErrorExceptionHandler() {}

  @Override
  public Response toResponse(ClientErrorException exception) {
    return handleException(
        exception,
        exception.getMessage(),
        exception.getResponse().getStatus(),
        Status.fromStatusCode(exception.getResponse().getStatus()).getReasonPhrase(),
        false);
  }
}
