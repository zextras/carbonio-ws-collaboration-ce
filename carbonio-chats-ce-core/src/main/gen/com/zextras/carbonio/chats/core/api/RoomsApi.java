package com.zextras.carbonio.chats.core.api;

import com.zextras.carbonio.chats.core.model.*;
import com.zextras.carbonio.chats.core.api.RoomsApiService;

import io.swagger.annotations.ApiParam;
import io.swagger.jaxrs.*;

import java.io.File;
import com.zextras.carbonio.chats.core.model.HashDto;
import com.zextras.carbonio.chats.core.model.IdDto;
import com.zextras.carbonio.chats.core.model.MemberDto;
import com.zextras.carbonio.chats.core.model.RoomCreationFieldsDto;
import com.zextras.carbonio.chats.core.model.RoomDto;
import com.zextras.carbonio.chats.core.model.RoomEditableFieldsDto;
import com.zextras.carbonio.chats.core.model.RoomInfoDto;
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

@Path("/rooms")


@io.swagger.annotations.Api(description = "the rooms API")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen")
public class RoomsApi  {

    @Inject RoomsApiService service;

    @POST
    @Path("/{roomId}/attachments")
    @Consumes({ "application/octet-stream" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Uploads a new attachment", notes = "", response = IdDto.class, tags={ "Attachments", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 201, message = "File identifier", response = IdDto.class),
        
        @io.swagger.annotations.ApiResponse(code = 413, message = "The request had a payload that was too big", response = Void.class) })
    public Response addAttachment( @PathParam("roomId") UUID roomId, @NotNull  @ApiParam(value = "file content type and file name writes with inline format ('fileName=<>;mimeType=<>')" ,required=true) @HeaderParam("X-Content-Disposition") String xContentDisposition,@ApiParam(value = "file stream" ,required=true) @NotNull @Valid File body,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.addAttachment(roomId,xContentDisposition,body,securityContext);
    }
    @PUT
    @Path("/{roomId}/members/{userId}/owner")
    
    
    @io.swagger.annotations.ApiOperation(value = "Promotes a member to owner", notes = "", response = Void.class, tags={ "Members", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 204, message = "The member was promoted", response = Void.class),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "The requested resource was not found", response = Void.class),
        
        @io.swagger.annotations.ApiResponse(code = 403, message = "The requester could not access the resource", response = Void.class) })
    public Response addOwner( @PathParam("roomId") UUID roomId, @PathParam("userId") UUID userId,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.addOwner(roomId,userId,securityContext);
    }
    @POST
    @Path("/{roomId}/members")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Adds or invites the specified user to the room", notes = "Adds the specified user to the room. This can only be performed by an of the given room ", response = MemberDto.class, tags={ "Rooms","Members", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 201, message = "The member added or invited", response = MemberDto.class),
        
        @io.swagger.annotations.ApiResponse(code = 400, message = "The request had wrong or missing parameters", response = Void.class),
        
        @io.swagger.annotations.ApiResponse(code = 403, message = "The requester could not access the resource", response = Void.class) })
    public Response addRoomMember( @PathParam("roomId") UUID roomId,@ApiParam(value = "member to add or invite" ,required=true) @NotNull @Valid MemberDto memberDto,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.addRoomMember(roomId,memberDto,securityContext);
    }
    @POST
    
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Creates a room of the specified type", notes = "", response = RoomInfoDto.class, tags={ "Rooms", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 201, message = "The newly created room", response = RoomInfoDto.class),
        
        @io.swagger.annotations.ApiResponse(code = 403, message = "The requester could not access the resource", response = Void.class),
        
        @io.swagger.annotations.ApiResponse(code = 400, message = "The request had wrong or missing parameters", response = Void.class) })
    public Response createRoom(@ApiParam(value = "room to create" ,required=true) @NotNull @Valid RoomCreationFieldsDto roomCreationFieldsDto,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.createRoom(roomCreationFieldsDto,securityContext);
    }
    @DELETE
    @Path("/{roomId}")
    
    
    @io.swagger.annotations.ApiOperation(value = "Deletes the specified room", notes = "", response = Void.class, tags={ "Rooms", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 204, message = "Room was deleted correctly or it never existed", response = Void.class),
        
        @io.swagger.annotations.ApiResponse(code = 403, message = "The requester could not access the resource", response = Void.class) })
    public Response deleteRoom( @PathParam("roomId") UUID roomId,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.deleteRoom(roomId,securityContext);
    }
    @GET
    @Path("/{roomId}")
    
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Retrieves the requested room", notes = "", response = RoomInfoDto.class, tags={ "Rooms", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "Requested room", response = RoomInfoDto.class),
        
        @io.swagger.annotations.ApiResponse(code = 403, message = "The requester could not access the resource", response = Void.class),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "The requested resource was not found", response = Void.class) })
    public Response getRoomById( @PathParam("roomId") UUID roomId,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.getRoomById(roomId,securityContext);
    }
    @GET
    @Path("/{roomId}/members")
    
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Retrieves every member to the given room", notes = "", response = MemberDto.class, responseContainer = "List", tags={ "Rooms","Members", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "The room members list", response = MemberDto.class, responseContainer = "List"),
        
        @io.swagger.annotations.ApiResponse(code = 403, message = "The requester could not access the resource", response = Void.class),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "The requested resource was not found", response = Void.class) })
    public Response getRoomMembers( @PathParam("roomId") UUID roomId,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.getRoomMembers(roomId,securityContext);
    }
    @GET
    
    
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Retrieves a list of every room the user has access to", notes = "", response = RoomDto.class, responseContainer = "List", tags={ "Rooms", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "List of every room that the user has access to", response = RoomDto.class, responseContainer = "List") })
    public Response getRooms(@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.getRooms(securityContext);
    }
    @PUT
    @Path("/{roomId}/mute")
    
    
    @io.swagger.annotations.ApiOperation(value = "Mutes notification for the specified room", notes = "", response = Void.class, tags={ "Rooms", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 204, message = "Room was muted correctly", response = Void.class),
        
        @io.swagger.annotations.ApiResponse(code = 403, message = "The requester could not access the resource", response = Void.class),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "The requested resource was not found", response = Void.class) })
    public Response muteRoom( @PathParam("roomId") UUID roomId,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.muteRoom(roomId,securityContext);
    }
    @DELETE
    @Path("/{roomId}/members/{userId}/owner")
    
    
    @io.swagger.annotations.ApiOperation(value = "Demotes a member from owner to normal member", notes = "", response = Void.class, tags={ "Members", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 204, message = "The member was demoted", response = Void.class),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "The requested resource was not found", response = Void.class),
        
        @io.swagger.annotations.ApiResponse(code = 403, message = "The requester could not access the resource", response = Void.class) })
    public Response removeOwner( @PathParam("roomId") UUID roomId, @PathParam("userId") UUID userId,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.removeOwner(roomId,userId,securityContext);
    }
    @DELETE
    @Path("/{roomId}/members/{userId}")
    
    
    @io.swagger.annotations.ApiOperation(value = "Removes a member from the room", notes = "Removes a member from the specified room. If the specified user is different from the requester, this action is considered as a kick ", response = Void.class, tags={ "Rooms","Members", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 204, message = "The member was deleted correctly or it never existed", response = Void.class),
        
        @io.swagger.annotations.ApiResponse(code = 403, message = "The requester could not access the resource", response = Void.class) })
    public Response removeRoomMember( @PathParam("roomId") UUID roomId, @PathParam("userId") UUID userId,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.removeRoomMember(roomId,userId,securityContext);
    }
    @PUT
    @Path("/{roomId}/hash")
    
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Resets the specified room hash", notes = "", response = HashDto.class, tags={ "Rooms", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "Room's hash", response = HashDto.class),
        
        @io.swagger.annotations.ApiResponse(code = 403, message = "The requester could not access the resource", response = Void.class),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "The requested resource was not found", response = Void.class) })
    public Response resetRoomHash( @PathParam("roomId") UUID roomId,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.resetRoomHash(roomId,securityContext);
    }
    @POST
    @Path("/{roomId}/picture")
    @Consumes({ "application/octet-stream" })
    
    @io.swagger.annotations.ApiOperation(value = "Uploads and sets a new room picture", notes = "", response = Void.class, tags={ "Rooms", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 204, message = "Room picture was changed correctly", response = Void.class) })
    public Response setRoomPicture( @PathParam("roomId") UUID roomId,@ApiParam(value = "image to set" ,required=true) @NotNull @Valid File body,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.setRoomPicture(roomId,body,securityContext);
    }
    @DELETE
    @Path("/{roomId}/mute")
    
    
    @io.swagger.annotations.ApiOperation(value = "Unmutes notification for the specified room", notes = "", response = Void.class, tags={ "Rooms", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 204, message = "Room was unmuted correctly", response = Void.class),
        
        @io.swagger.annotations.ApiResponse(code = 403, message = "The requester could not access the resource", response = Void.class),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "The requested resource was not found", response = Void.class) })
    public Response unmuteRoom( @PathParam("roomId") UUID roomId,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.unmuteRoom(roomId,securityContext);
    }
    @PUT
    @Path("/{roomId}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Updates a room information", notes = "", response = RoomDto.class, tags={ "Rooms", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "Updated room", response = RoomDto.class),
        
        @io.swagger.annotations.ApiResponse(code = 400, message = "The request had wrong or missing parameters", response = Void.class),
        
        @io.swagger.annotations.ApiResponse(code = 403, message = "The requester could not access the resource", response = Void.class),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "The requested resource was not found", response = Void.class) })
    public Response updateRoom( @PathParam("roomId") UUID roomId,@ApiParam(value = "room fields to update" ,required=true) @NotNull @Valid RoomEditableFieldsDto roomEditableFieldsDto,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.updateRoom(roomId,roomEditableFieldsDto,securityContext);
    }
}
