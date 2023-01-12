// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.exceptions;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.ValidationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

@Singleton
@Provider
public class ValidationExceptionHandler extends ExceptionHandler<ValidationException> {

  @Inject
  public ValidationExceptionHandler() {
  }

  @Override
  public Response toResponse(ValidationException exception) {
    return handleException(exception, exception.toString(), Status.BAD_REQUEST.getStatusCode(),
      Status.BAD_REQUEST.getReasonPhrase(), false);
  }
}
