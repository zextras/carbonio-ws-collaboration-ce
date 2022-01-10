package com.zextras.chats.core.model;

import java.util.Objects;
import java.util.ArrayList;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.UUID;
import javax.validation.constraints.*;
import io.swagger.annotations.*;

@ApiModel(description="Attachment of a message")@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen")
public class AttachmentDto   {
  
  private UUID id;
  private String name;
  private Long size;
  private String mimeType;
  private UUID userId;
  private UUID roomId;

  /**
   * identifier
   **/
  
  @ApiModelProperty(required = true, value = "identifier")
  @JsonProperty("id")
  @NotNull
  public UUID getId() {
    return id;
  }
  public void setId(UUID id) {
    this.id = id;
  }

  /**
   * file name
   **/
  
  @ApiModelProperty(required = true, value = "file name")
  @JsonProperty("name")
  @NotNull
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  /**
   * file length
   **/
  
  @ApiModelProperty(required = true, value = "file length")
  @JsonProperty("size")
  @NotNull
  public Long getSize() {
    return size;
  }
  public void setSize(Long size) {
    this.size = size;
  }

  /**
   * mime type
   **/
  
  @ApiModelProperty(required = true, value = "mime type")
  @JsonProperty("mimeType")
  @NotNull
  public String getMimeType() {
    return mimeType;
  }
  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  /**
   * identifier of updated user
   **/
  
  @ApiModelProperty(required = true, value = "identifier of updated user")
  @JsonProperty("userId")
  @NotNull
  public UUID getUserId() {
    return userId;
  }
  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  /**
   * identifier of destination room
   **/
  
  @ApiModelProperty(required = true, value = "identifier of destination room")
  @JsonProperty("roomId")
  @NotNull
  public UUID getRoomId() {
    return roomId;
  }
  public void setRoomId(UUID roomId) {
    this.roomId = roomId;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AttachmentDto attachment = (AttachmentDto) o;
    return Objects.equals(id, attachment.id) &&
        Objects.equals(name, attachment.name) &&
        Objects.equals(size, attachment.size) &&
        Objects.equals(mimeType, attachment.mimeType) &&
        Objects.equals(userId, attachment.userId) &&
        Objects.equals(roomId, attachment.roomId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, size, mimeType, userId, roomId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AttachmentDto {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    size: ").append(toIndentedString(size)).append("\n");
    sb.append("    mimeType: ").append(toIndentedString(mimeType)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    roomId: ").append(toIndentedString(roomId)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

