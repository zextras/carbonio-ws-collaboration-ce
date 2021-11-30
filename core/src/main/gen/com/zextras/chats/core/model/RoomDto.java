package com.zextras.chats.core.model;

import com.zextras.chats.core.invoker.StringUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.zextras.chats.core.utils.CustomLocalDateTimeDeserializer;
import com.zextras.chats.core.utils.CustomLocalDateTimeSerializer;
import java.util.Objects;
import java.util.ArrayList;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.zextras.chats.core.model.RoomAllOfDto;
import com.zextras.chats.core.model.RoomCreationFieldsDto;
import com.zextras.chats.core.model.RoomTypeDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.validation.constraints.*;

/**
 * Room data
 */
@ApiModel(description = "Room data")
@Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoomDto {

  /**
   * room name
  **/
  @ApiModelProperty(required = true, value = "room name")
  @JsonProperty("name") @NotNull
  private String name;

  /**
   * room description
  **/
  @ApiModelProperty(required = true, value = "room description")
  @JsonProperty("description") @NotNull
  private String description;

  @ApiModelProperty(required = true, value = "")
  @JsonProperty("type") @NotNull
  private RoomTypeDto type;

  /**
   * room identifier
  **/
  @ApiModelProperty(required = true, value = "room identifier")
  @JsonProperty("id") @NotNull
  private UUID id;

  /**
   * an hash that can be used to compose the room&#39;s link
  **/
  @ApiModelProperty(required = true, value = "an hash that can be used to compose the room's link")
  @JsonProperty("hash") @NotNull
  private String hash;

  /**
   * creation date
  **/
  @ApiModelProperty(required = true, value = "creation date")
  @JsonProperty("createdAt") @NotNull
  @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
  @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
  private LocalDateTime createdAt;

  /**
   * update date
  **/
  @ApiModelProperty(required = true, value = "update date")
  @JsonProperty("updatedAt") @NotNull
  @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
  @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
  private LocalDateTime updatedAt;

  /**
   * password to room access
  **/
  @ApiModelProperty(value = "password to room access")
  @JsonProperty("password") 
  private String password;

  public static RoomDto create() {
    return new RoomDto();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public RoomDto name(String name) {
    this.name = name;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public RoomDto description(String description) {
    this.description = description;
    return this;
  }

  public RoomTypeDto getType() {
    return type;
  }

  public void setType(RoomTypeDto type) {
    this.type = type;
  }

  public RoomDto type(RoomTypeDto type) {
    this.type = type;
    return this;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public RoomDto id(UUID id) {
    this.id = id;
    return this;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public RoomDto hash(String hash) {
    this.hash = hash;
    return this;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public RoomDto createdAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public RoomDto updatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public RoomDto password(String password) {
    this.password = password;
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
    RoomDto room = (RoomDto) o;
    return Objects.equals(this.name, room.name) &&
      Objects.equals(this.description, room.description) &&
      Objects.equals(this.type, room.type) &&
      Objects.equals(this.id, room.id) &&
      Objects.equals(this.hash, room.hash) &&
      Objects.equals(this.createdAt, room.createdAt) &&
      Objects.equals(this.updatedAt, room.updatedAt) &&
      Objects.equals(this.password, room.password);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, description, type, id, hash, createdAt, updatedAt, password);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RoomDto {\n");
    sb.append("  name: ").append(StringUtil.toIndentedString(name)).append("\n");
    sb.append("  description: ").append(StringUtil.toIndentedString(description)).append("\n");
    sb.append("  type: ").append(StringUtil.toIndentedString(type)).append("\n");
    sb.append("  id: ").append(StringUtil.toIndentedString(id)).append("\n");
    sb.append("  hash: ").append(StringUtil.toIndentedString(hash)).append("\n");
    sb.append("  createdAt: ").append(StringUtil.toIndentedString(createdAt)).append("\n");
    sb.append("  updatedAt: ").append(StringUtil.toIndentedString(updatedAt)).append("\n");
    sb.append("  password: ").append(StringUtil.toIndentedString(password)).append("\n");
    sb.append("}");
    return sb.toString();
  }
}
