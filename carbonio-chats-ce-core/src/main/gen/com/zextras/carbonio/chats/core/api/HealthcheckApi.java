package com.zextras.carbonio.chats.core.api;

import com.zextras.carbonio.chats.core.model.*;
import com.zextras.carbonio.chats.core.api.HealthcheckApiService;

import io.swagger.annotations.ApiParam;
import io.swagger.jaxrs.*;


import java.util.Map;
import java.util.List;
import com.zextras.carbonio.chats.core.api.NotFoundException;

import java.io.InputStream;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.*;
import javax.inject.Inject;

import javax.validation.constraints.*;
import javax.validation.Valid;

@Path("/healthcheck")


@io.swagger.annotations.Api(description = "the healthcheck API")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen")
public class HealthcheckApi  {

    @Inject HealthcheckApiService service;

    @GET
    
    
    
    @io.swagger.annotations.ApiOperation(value = "healthcheck endpoint which will answer according to the service state", notes = "", response = Void.class, tags={ "Operations", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "Everything is operational", response = Void.class) })
    public Response healthcheck(@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.healthcheck(securityContext);
    }
}
