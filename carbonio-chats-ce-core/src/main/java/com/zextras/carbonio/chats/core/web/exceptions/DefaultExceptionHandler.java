// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.exceptions;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

@Singleton
@Provider
public class DefaultExceptionHandler extends ExceptionHandler<Exception> {

  @Inject
  public DefaultExceptionHandler() {
  }

  @Override
  public Response toResponse(Exception exception) {
    return handleException(exception, exception.getMessage(), Status.INTERNAL_SERVER_ERROR, true);
  }
}