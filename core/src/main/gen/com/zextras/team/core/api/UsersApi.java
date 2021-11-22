package com.zextras.team.core.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.UUID;
import com.zextras.team.core.model.UserDto;
import javax.annotation.Generated;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.*;
import javax.inject.Inject;

import javax.validation.constraints.*;
import javax.validation.Valid;

@Path("/users/{userId}")
@Api(description = "the users API")
@Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen", date = "2021-11-22T10:48:21.557692+01:00[Europe/Rome]")
public class UsersApi  {

  private final UsersApiService service;

  @Inject
  public UsersApi (UsersApiService service) {
    this.service = service;
  }

  @GET
  @Produces({ "application/json" })
  @ApiOperation(value = "Retrieves a user", response = UserDto.class, tags = { "Users" })
  @ApiResponses(value = { 
    @ApiResponse(code = 200, message = "Requested user", response = UserDto.class),
    @ApiResponse(code = 404, message = "The requested resource was not found")
  })
  public Response getUserById( @PathParam("userId") UUID userId, @Context SecurityContext securityContext) {
    return Response.status(200).entity(service.getUserById(userId, securityContext)).build();
  }
}
