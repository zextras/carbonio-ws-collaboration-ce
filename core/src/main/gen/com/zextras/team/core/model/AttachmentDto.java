package com.zextras.team.core.model;

import com.zextras.team.core.invoker.StringUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;
import java.util.ArrayList;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.UUID;
import javax.validation.constraints.*;

/**
 * Attachment of a message
 */
@ApiModel(description = "Attachment of a message")
@Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen", date = "2021-11-22T10:48:21.557692+01:00[Europe/Rome]")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttachmentDto {

  private UUID id;
  private String name;
  private Integer size;
  private String mimeType;
  private UUID userId;
  private UUID roomId;

  public static AttachmentDto create() {
    return new AttachmentDto();
  }

  /**
   * identifier
  **/
  @ApiModelProperty(required = true, value = "identifier")
  @JsonProperty("id") @NotNull
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public AttachmentDto id(UUID id) {
    this.id = id;
    return this;
  }

  /**
   * file name
  **/
  @ApiModelProperty(required = true, value = "file name")
  @JsonProperty("name") @NotNull
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public AttachmentDto name(String name) {
    this.name = name;
    return this;
  }

  /**
   * file length
  **/
  @ApiModelProperty(required = true, value = "file length")
  @JsonProperty("size") @NotNull
  public Integer getSize() {
    return size;
  }

  public void setSize(Integer size) {
    this.size = size;
  }

  public AttachmentDto size(Integer size) {
    this.size = size;
    return this;
  }

  /**
   * mime type
  **/
  @ApiModelProperty(required = true, value = "mime type")
  @JsonProperty("mimeType") @NotNull
  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public AttachmentDto mimeType(String mimeType) {
    this.mimeType = mimeType;
    return this;
  }

  /**
   * identifier of updated user
  **/
  @ApiModelProperty(required = true, value = "identifier of updated user")
  @JsonProperty("userId") @NotNull
  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public AttachmentDto userId(UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * identifier of destination room
  **/
  @ApiModelProperty(required = true, value = "identifier of destination room")
  @JsonProperty("roomId") @NotNull
  public UUID getRoomId() {
    return roomId;
  }

  public void setRoomId(UUID roomId) {
    this.roomId = roomId;
  }

  public AttachmentDto roomId(UUID roomId) {
    this.roomId = roomId;
    return this;
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
    return Objects.equals(this.id, attachment.id) &&
      Objects.equals(this.name, attachment.name) &&
      Objects.equals(this.size, attachment.size) &&
      Objects.equals(this.mimeType, attachment.mimeType) &&
      Objects.equals(this.userId, attachment.userId) &&
      Objects.equals(this.roomId, attachment.roomId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, size, mimeType, userId, roomId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AttachmentDto {\n");
    sb.append("  id: ").append(StringUtil.toIndentedString(id)).append("\n");
    sb.append("  name: ").append(StringUtil.toIndentedString(name)).append("\n");
    sb.append("  size: ").append(StringUtil.toIndentedString(size)).append("\n");
    sb.append("  mimeType: ").append(StringUtil.toIndentedString(mimeType)).append("\n");
    sb.append("  userId: ").append(StringUtil.toIndentedString(userId)).append("\n");
    sb.append("  roomId: ").append(StringUtil.toIndentedString(roomId)).append("\n");
    sb.append("}");
    return sb.toString();
  }
}
