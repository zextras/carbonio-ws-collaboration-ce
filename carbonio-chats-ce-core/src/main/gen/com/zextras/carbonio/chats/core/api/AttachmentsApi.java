package com.zextras.carbonio.chats.core.api;

import com.zextras.carbonio.chats.core.model.*;
import com.zextras.carbonio.chats.core.api.AttachmentsApiService;

import io.swagger.annotations.ApiParam;
import io.swagger.jaxrs.*;

import com.zextras.carbonio.chats.core.model.AttachmentDto;
import java.io.File;
import java.util.UUID;

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

@Path("/attachments/{fileId}")


@io.swagger.annotations.Api(description = "the attachments API")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen")
public class AttachmentsApi  {

    @Inject AttachmentsApiService service;

    @DELETE
    
    
    
    @io.swagger.annotations.ApiOperation(value = "Deletes an uploaded attachment", notes = "", response = Void.class, tags={ "Attachments", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 204, message = "The file was deleted correctly", response = Void.class),
        
        @io.swagger.annotations.ApiResponse(code = 403, message = "The requester could not access the resource", response = Void.class) })
    public Response deleteAttachment( @PathParam("fileId") UUID fileId,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.deleteAttachment(fileId,securityContext);
    }
    @GET
    @Path("/download")
    
    @Produces({ "application/octet-stream" })
    @io.swagger.annotations.ApiOperation(value = "Retrieves an uploaded attachment", notes = "", response = File.class, tags={ "Attachments", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "The requested file", response = File.class),
        
        @io.swagger.annotations.ApiResponse(code = 403, message = "The requester could not access the resource", response = Void.class),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "The requested resource was not found", response = Void.class) })
    public Response getAttachment( @PathParam("fileId") UUID fileId,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.getAttachment(fileId,securityContext);
    }
    @GET
    
    
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Retrieves info related to an uploaded attachment", notes = "", response = AttachmentDto.class, tags={ "Attachments", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "Attachment informations", response = AttachmentDto.class),
        
        @io.swagger.annotations.ApiResponse(code = 403, message = "The requester could not access the resource", response = Void.class),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "The requested resource was not found", response = Void.class) })
    public Response getAttachmentInfo( @PathParam("fileId") UUID fileId,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.getAttachmentInfo(fileId,securityContext);
    }
    @GET
    @Path("/preview")
    
    @Produces({ "application/octet-stream" })
    @io.swagger.annotations.ApiOperation(value = "Retrieves the prefiew of an uploaded attachment", notes = "", response = File.class, tags={ "Attachments", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "The requested file preview", response = File.class),
        
        @io.swagger.annotations.ApiResponse(code = 403, message = "The requester could not access the resource", response = Void.class),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "The requested resource was not found", response = Void.class) })
    public Response getAttachmentPreview( @PathParam("fileId") UUID fileId,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.getAttachmentPreview(fileId,securityContext);
    }
}
