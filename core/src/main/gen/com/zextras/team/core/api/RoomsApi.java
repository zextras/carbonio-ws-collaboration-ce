package com.zextras.team.core.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.File;
import com.zextras.team.core.model.HashDto;
import com.zextras.team.core.model.IdDto;
import com.zextras.team.core.model.MemberDto;
import com.zextras.team.core.model.RoomCreationFieldsDto;
import com.zextras.team.core.model.RoomDto;
import com.zextras.team.core.model.RoomEditableFieldsDto;
import com.zextras.team.core.model.RoomInfoDto;
import java.util.UUID;
import javax.annotation.Generated;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.*;
import javax.inject.Inject;

import javax.validation.constraints.*;
import javax.validation.Valid;

@Path("/rooms")
@Api(description = "the rooms API")
@Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen", date = "2021-11-22T10:48:21.557692+01:00[Europe/Rome]")
public class RoomsApi  {

  private final RoomsApiService service;

  @Inject
  public RoomsApi (RoomsApiService service) {
    this.service = service;
  }

  @PUT
  @Path("/{roomId}/members/{userId}/owner")
  @ApiOperation(value = "Promotes a member to owner", tags = { "Members" })
  @ApiResponses(value = { 
    @ApiResponse(code = 204, message = "The member was promoted"),
    @ApiResponse(code = 404, message = "The requested resource was not found"),
    @ApiResponse(code = 403, message = "The requester could not access the resource")
  })
  public Response addOwner( @PathParam("roomId") UUID roomId,  @PathParam("userId") UUID userId, @Context SecurityContext securityContext) {
    service.addOwner(roomId, userId, securityContext);
    return Response.status(204).build();
  }

  @POST
  @Path("/{roomId}/members/{userid}")
  @Consumes({ "application/json" })
  @Produces({ "application/json" })
  @ApiOperation(value = "Adds or invites the specified user to the room", notes = "Adds the specified user to the room. If the specified user is different from the requester, this action is considered as an invitation ", response = MemberDto.class, tags = { "Rooms", "Members" })
  @ApiResponses(value = { 
    @ApiResponse(code = 201, message = "The member added or invited", response = MemberDto.class),
    @ApiResponse(code = 400, message = "The request had wrong or missing parameters")
  })
  public Response addRoomMember( @PathParam("roomId") UUID roomId,  @PathParam("userid") UUID userid, @ApiParam(value = "member to add or invite" , required = true) @NotNull @Valid MemberDto memberDto, @Context SecurityContext securityContext) {
    return Response.status(201).entity(service.addRoomMember(roomId, userid, memberDto, securityContext)).build();
  }

  @POST
  @Consumes({ "application/json" })
  @Produces({ "application/json" })
  @ApiOperation(value = "Creates a room of the specified type", response = RoomInfoDto.class, tags = { "Rooms" })
  @ApiResponses(value = { 
    @ApiResponse(code = 201, message = "The newly created room", response = RoomInfoDto.class),
    @ApiResponse(code = 403, message = "The requester could not access the resource"),
    @ApiResponse(code = 400, message = "The request had wrong or missing parameters")
  })
  public Response createRoom(@ApiParam(value = "room to create" , required = true) @NotNull @Valid RoomCreationFieldsDto roomCreationFieldsDto, @Context SecurityContext securityContext) {
    return Response.status(201).entity(service.createRoom(roomCreationFieldsDto, securityContext)).build();
  }

  @DELETE
  @Path("/{roomId}")
  @ApiOperation(value = "Deletes the specified room", tags = { "Rooms" })
  @ApiResponses(value = { 
    @ApiResponse(code = 204, message = "Room was deleted correctly or it never existed"),
    @ApiResponse(code = 403, message = "The requester could not access the resource")
  })
  public Response deleteRoom( @PathParam("roomId") UUID roomId, @Context SecurityContext securityContext) {
    service.deleteRoom(roomId, securityContext);
    return Response.status(204).build();
  }

  @DELETE
  @Path("/{roomId}/members/{userid}")
  @ApiOperation(value = "Removes a member to the room", notes = "Removes a member from the specified room. If the specified user is different from the requester, this action is considered as a kick ", tags = { "Rooms", "Members" })
  @ApiResponses(value = { 
    @ApiResponse(code = 204, message = "The member was deleted correctly or it never existed"),
    @ApiResponse(code = 403, message = "The requester could not access the resource")
  })
  public Response deleteRoomMember( @PathParam("roomId") UUID roomId,  @PathParam("userid") UUID userid, @Context SecurityContext securityContext) {
    service.deleteRoomMember(roomId, userid, securityContext);
    return Response.status(204).build();
  }

  @GET
  @Path("/{roomId}")
  @Produces({ "application/json" })
  @ApiOperation(value = "Retrieves the requested room", response = RoomInfoDto.class, tags = { "Rooms" })
  @ApiResponses(value = { 
    @ApiResponse(code = 200, message = "Requested room", response = RoomInfoDto.class),
    @ApiResponse(code = 403, message = "The requester could not access the resource"),
    @ApiResponse(code = 404, message = "The requested resource was not found")
  })
  public Response getRoomById( @PathParam("roomId") UUID roomId, @Context SecurityContext securityContext) {
    return Response.status(200).entity(service.getRoomById(roomId, securityContext)).build();
  }

  @GET
  @Path("/{roomId}/members")
  @Produces({ "application/json" })
  @ApiOperation(value = "Retrieves every member to the given room", response = MemberDto.class, responseContainer = "List", tags = { "Rooms", "Members" })
  @ApiResponses(value = { 
    @ApiResponse(code = 200, message = "The room members list", response = MemberDto.class, responseContainer = "List"),
    @ApiResponse(code = 403, message = "The requester could not access the resource"),
    @ApiResponse(code = 404, message = "The requested resource was not found")
  })
  public Response getRoomMembers( @PathParam("roomId") UUID roomId, @Context SecurityContext securityContext) {
    return Response.status(200).entity(service.getRoomMembers(roomId, securityContext)).build();
  }

  @GET
  @Produces({ "application/json" })
  @ApiOperation(value = "Retrieves a list of every room the user has access to", response = RoomDto.class, responseContainer = "List", tags = { "Rooms" })
  @ApiResponses(value = { 
    @ApiResponse(code = 200, message = "List of every room that the user has access to", response = RoomDto.class, responseContainer = "List")
  })
  public Response getRooms(@Context SecurityContext securityContext) {
    return Response.status(200).entity(service.getRooms(securityContext)).build();
  }

  @PUT
  @Path("/{roomId}/mute")
  @ApiOperation(value = "Mutes notification for the specified room", tags = { "Rooms" })
  @ApiResponses(value = { 
    @ApiResponse(code = 204, message = "Room was muted correctly"),
    @ApiResponse(code = 403, message = "The requester could not access the resource"),
    @ApiResponse(code = 404, message = "The requested resource was not found")
  })
  public Response muteRoom( @PathParam("roomId") UUID roomId, @Context SecurityContext securityContext) {
    service.muteRoom(roomId, securityContext);
    return Response.status(204).build();
  }

  @DELETE
  @Path("/{roomId}/members/{userId}/owner")
  @ApiOperation(value = "Demotes a member from owner to normal member", tags = { "Members" })
  @ApiResponses(value = { 
    @ApiResponse(code = 204, message = "The member was demoted"),
    @ApiResponse(code = 404, message = "The requested resource was not found"),
    @ApiResponse(code = 403, message = "The requester could not access the resource")
  })
  public Response removeOwner( @PathParam("roomId") UUID roomId,  @PathParam("userId") UUID userId, @Context SecurityContext securityContext) {
    service.removeOwner(roomId, userId, securityContext);
    return Response.status(204).build();
  }

  @PUT
  @Path("/{roomId}/hash")
  @Produces({ "application/json" })
  @ApiOperation(value = "Resets the specified room hash", response = HashDto.class, tags = { "Rooms" })
  @ApiResponses(value = { 
    @ApiResponse(code = 200, message = "Room's hash", response = HashDto.class),
    @ApiResponse(code = 403, message = "The requester could not access the resource"),
    @ApiResponse(code = 404, message = "The requested resource was not found")
  })
  public Response resetRoomHash( @PathParam("roomId") UUID roomId, @Context SecurityContext securityContext) {
    return Response.status(200).entity(service.resetRoomHash(roomId, securityContext)).build();
  }

  @POST
  @Path("/{roomId}/picture")
  @Consumes({ "application/octet-stream" })
  @ApiOperation(value = "Uploads and sets a new room picture", tags = { "Rooms" })
  @ApiResponses(value = { 
    @ApiResponse(code = 204, message = "Room picture was changed correctly")
  })
  public Response setRoomPicture( @PathParam("roomId") UUID roomId, @ApiParam(value = "image to set" , required = true) @NotNull @Valid File body, @Context SecurityContext securityContext) {
    service.setRoomPicture(roomId, body, securityContext);
    return Response.status(204).build();
  }

  @DELETE
  @Path("/{roomId}/mute")
  @ApiOperation(value = "Unmutes notification for the specified room", tags = { "Rooms" })
  @ApiResponses(value = { 
    @ApiResponse(code = 204, message = "Room was unmuted correctly"),
    @ApiResponse(code = 403, message = "The requester could not access the resource"),
    @ApiResponse(code = 404, message = "The requested resource was not found")
  })
  public Response unmuteRoom( @PathParam("roomId") UUID roomId, @Context SecurityContext securityContext) {
    service.unmuteRoom(roomId, securityContext);
    return Response.status(204).build();
  }

  @PUT
  @Path("/{roomId}")
  @Consumes({ "application/json" })
  @Produces({ "application/json" })
  @ApiOperation(value = "Updates a room information", response = RoomDto.class, tags = { "Rooms" })
  @ApiResponses(value = { 
    @ApiResponse(code = 200, message = "Updated room", response = RoomDto.class),
    @ApiResponse(code = 400, message = "The request had wrong or missing parameters"),
    @ApiResponse(code = 403, message = "The requester could not access the resource"),
    @ApiResponse(code = 404, message = "The requested resource was not found")
  })
  public Response updateRoom( @PathParam("roomId") UUID roomId, @ApiParam(value = "room fields to update" , required = true) @NotNull @Valid RoomEditableFieldsDto roomEditableFieldsDto, @Context SecurityContext securityContext) {
    return Response.status(200).entity(service.updateRoom(roomId, roomEditableFieldsDto, securityContext)).build();
  }

  @POST
  @Path("/{roomId}/attachments")
  @Consumes({ "application/octet-stream" })
  @Produces({ "application/json" })
  @ApiOperation(value = "Uploads a new attachment", response = IdDto.class, tags = { "Attachments" })
  @ApiResponses(value = { 
    @ApiResponse(code = 201, message = "File identifier", response = IdDto.class),
    @ApiResponse(code = 413, message = "The request had a payload that was too big")
  })
  public Response uploadAttachment( @PathParam("roomId") UUID roomId, @ApiParam(value = "file stream" , required = true) @NotNull @Valid File body, @Context SecurityContext securityContext) {
    return Response.status(201).entity(service.uploadAttachment(roomId, body, securityContext)).build();
  }
}
