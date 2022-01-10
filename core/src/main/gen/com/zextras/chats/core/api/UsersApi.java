package com.zextras.chats.core.api;

import com.zextras.chats.core.model.*;
import com.zextras.chats.core.api.UsersApiService;

import io.swagger.annotations.ApiParam;
import io.swagger.jaxrs.*;

import java.util.UUID;
import com.zextras.chats.core.model.UserDto;

import java.util.Map;
import java.util.List;
import com.zextras.chats.core.api.NotFoundException;

import java.io.InputStream;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.*;
import javax.inject.Inject;

import javax.validation.constraints.*;
import javax.validation.Valid;

@Path("/users/{userId}")


@io.swagger.annotations.Api(description = "the users API")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen")
public class UsersApi  {

    @Inject UsersApiService service;

    @GET
    
    
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Retrieves a user", notes = "", response = UserDto.class, tags={ "Users", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "Requested user", response = UserDto.class),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "The requested resource was not found", response = Void.class) })
    public Response getUserById( @PathParam("userId") UUID userId,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.getUserById(userId,securityContext);
    }
}
