// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.api.versioning;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/dummy/versioned/api")
@Api(description = "Some dummy API for test")
public class DummyVersionedApi {

  @GET
  @Produces({"application/json"})
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "Get dummy model", response = DummyModel.class),
        @ApiResponse(code = 401, message = "User not authorized", response = Void.class),
        @ApiResponse(
            code = 404,
            message = "The requested resource was not found",
            response = Void.class)
      })
  public Response getResource(@Context SecurityContext securityContext) {
    return Response.ok().entity(new DummyModel("John Doe", "+123456789")).build();
  }

  @POST
  @Produces({"application/json"})
  @Consumes({"application/json"})
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "Add dummy model", response = DummyModel.class),
        @ApiResponse(code = 401, message = "User not authorized", response = Void.class),
        @ApiResponse(
            code = 404,
            message = "The requested resource was not found",
            response = Void.class)
      })
  public Response addResource(
      @ApiParam(value = "Data to create a new dummy model", required = true) @NotNull @Valid
          DummyModel dummyModel,
      @Context SecurityContext securityContext) {
    return Response.ok().entity(dummyModel).build();
  }
}
