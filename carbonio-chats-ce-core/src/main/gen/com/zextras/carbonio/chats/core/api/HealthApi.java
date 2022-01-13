package com.zextras.carbonio.chats.core.api;

import com.zextras.carbonio.chats.core.model.*;
import com.zextras.carbonio.chats.core.api.HealthApiService;

import io.swagger.annotations.ApiParam;
import io.swagger.jaxrs.*;

import com.zextras.carbonio.chats.core.model.HealthResponseDto;

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

@Path("/health")


@io.swagger.annotations.Api(description = "the health API")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen")
public class HealthApi  {

    @Inject HealthApiService service;

    @GET
    
    
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Returns the general service status", notes = "", response = HealthResponseDto.class, tags={ "Health", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "General status of the service and its dependencies", response = HealthResponseDto.class) })
    public Response healthInfo(@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.healthInfo(securityContext);
    }
    @GET
    @Path("/live")
    
    
    @io.swagger.annotations.ApiOperation(value = "Returns 200 if the service is alive", notes = "", response = Void.class, tags={ "Health", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "", response = Void.class) })
    public Response isLive(@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.isLive(securityContext);
    }
    @GET
    @Path("/ready")
    
    
    @io.swagger.annotations.ApiOperation(value = "Returns 200 if the service is ready to receive requests", notes = "", response = Void.class, tags={ "Health", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "", response = Void.class),
        
        @io.swagger.annotations.ApiResponse(code = 500, message = "The service is not yet ready to receive requests", response = Void.class) })
    public Response isReady(@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.isReady(securityContext);
    }
}
