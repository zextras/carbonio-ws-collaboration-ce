// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.exceptions;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;

@Singleton
@Provider
public class ValidationExceptionHandler extends ExceptionHandler<ValidationException> {

  @Inject
  public ValidationExceptionHandler() {}

  @Override
  public Response toResponse(ValidationException exception) {
    return handleException(
        exception,
        exception.toString(),
        Status.BAD_REQUEST.getStatusCode(),
        Status.BAD_REQUEST.getReasonPhrase(),
        false);
  }
}
