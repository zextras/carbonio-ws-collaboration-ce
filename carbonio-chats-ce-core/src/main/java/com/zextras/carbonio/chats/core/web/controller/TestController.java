// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.controller;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Singleton
@Path("/test")
public class TestController {

  @Inject
  public TestController() {
  }

  @GET
  @Path("/{id}")
  @Produces({MediaType.APPLICATION_JSON})
  public String get(@PathParam("id") String id, @QueryParam("param") List<String> params) {
    return params.get(100);
  }
}
