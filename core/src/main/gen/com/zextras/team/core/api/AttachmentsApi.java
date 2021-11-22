package com.zextras.team.core.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import com.zextras.team.core.model.AttachmentDto;
import java.io.File;
import java.util.UUID;
import javax.annotation.Generated;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.*;
import javax.inject.Inject;

import javax.validation.constraints.*;
import javax.validation.Valid;

@Path("/attachments/{fileId}")
@Api(description = "the attachments API")
@Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen", date = "2021-11-22T10:48:21.557692+01:00[Europe/Rome]")
public class AttachmentsApi  {

  private final AttachmentsApiService service;

  @Inject
  public AttachmentsApi (AttachmentsApiService service) {
    this.service = service;
  }

  @DELETE
  @ApiOperation(value = "Deletes an uploaded attachment", tags = { "Attachments" })
  @ApiResponses(value = { 
    @ApiResponse(code = 204, message = "The file was deleted correctly"),
    @ApiResponse(code = 403, message = "The requester could not access the resource")
  })
  public Response deleteAttachment( @PathParam("fileId") UUID fileId, @Context SecurityContext securityContext) {
    service.deleteAttachment(fileId, securityContext);
    return Response.status(204).build();
  }

  @GET
  @Path("/download")
  @Produces({ "application/octet-stream" })
  @ApiOperation(value = "Retrieves an uploaded attachment", response = File.class, tags = { "Attachments" })
  @ApiResponses(value = { 
    @ApiResponse(code = 200, message = "The requested file", response = File.class),
    @ApiResponse(code = 403, message = "The requester could not access the resource"),
    @ApiResponse(code = 404, message = "The requested resource was not found")
  })
  public Response getAttachment( @PathParam("fileId") UUID fileId, @Context SecurityContext securityContext) {
    return Response.status(200).entity(service.getAttachment(fileId, securityContext)).build();
  }

  @GET
  @Produces({ "application/json" })
  @ApiOperation(value = "Retrieves info related to an uploaded attachment", response = AttachmentDto.class, tags = { "Attachments" })
  @ApiResponses(value = { 
    @ApiResponse(code = 200, message = "Attachment informations", response = AttachmentDto.class),
    @ApiResponse(code = 403, message = "The requester could not access the resource"),
    @ApiResponse(code = 404, message = "The requested resource was not found")
  })
  public Response getAttachmentInfo( @PathParam("fileId") UUID fileId, @Context SecurityContext securityContext) {
    return Response.status(200).entity(service.getAttachmentInfo(fileId, securityContext)).build();
  }

  @GET
  @Path("/preview")
  @Produces({ "application/octet-stream" })
  @ApiOperation(value = "Retrieves the prefiew of an uploaded attachment", response = File.class, tags = { "Attachments" })
  @ApiResponses(value = { 
    @ApiResponse(code = 200, message = "The requested file preview", response = File.class),
    @ApiResponse(code = 403, message = "The requester could not access the resource"),
    @ApiResponse(code = 404, message = "The requested resource was not found")
  })
  public Response getAttachmentPreview( @PathParam("fileId") UUID fileId, @Context SecurityContext securityContext) {
    return Response.status(200).entity(service.getAttachmentPreview(fileId, securityContext)).build();
  }
}
