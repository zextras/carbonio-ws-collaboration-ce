package com.zextras.chats.core.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.annotation.Generated;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.*;
import javax.inject.Inject;

import javax.validation.constraints.*;
import javax.validation.Valid;

@Path("/healthcheck")
@Api(description = "the healthcheck API")
@Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen")
public class HealthcheckApi  {

  private final HealthcheckApiService service;

  @Inject
  public HealthcheckApi (HealthcheckApiService service) {
    this.service = service;
  }

  @GET
  @ApiOperation(value = "healthcheck endpoint which will answer according to the service state", tags = { "Operations" })
  @ApiResponses(value = { 
    @ApiResponse(code = 200, message = "Everything is operational")
  })
  public Response healthcheck(@Context SecurityContext securityContext) {
    service.healthcheck(securityContext);
    return Response.status(200).build();
  }
}
