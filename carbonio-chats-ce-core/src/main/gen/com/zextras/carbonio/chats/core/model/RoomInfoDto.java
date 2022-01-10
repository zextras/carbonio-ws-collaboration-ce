package com.zextras.carbonio.chats.core.model;

import java.util.Objects;
import java.util.ArrayList;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.zextras.carbonio.chats.core.model.MemberDto;
import com.zextras.carbonio.chats.core.model.RoomDto;
import com.zextras.carbonio.chats.core.model.RoomTypeDto;
import com.zextras.carbonio.chats.core.model.RoomUserSettingsDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.validation.constraints.*;
import io.swagger.annotations.*;

@ApiModel(description="Room data with members list and current user settings")@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen")
public class RoomInfoDto   {
  
  private List<MemberDto> members = new ArrayList<>();
  private RoomUserSettingsDto userSettings;
  private String name;
  private String description;
  private RoomTypeDto type;
  private UUID id;
  private String hash;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;
  private String password;

  /**
   * list of users subscribed to the room
   **/
  
  @ApiModelProperty(required = true, value = "list of users subscribed to the room")
  @JsonProperty("members")
  @NotNull
  public List<MemberDto> getMembers() {
    return members;
  }
  public void setMembers(List<MemberDto> members) {
    this.members = members;
  }

  /**
   **/
  
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("userSettings")
  @NotNull
  public RoomUserSettingsDto getUserSettings() {
    return userSettings;
  }
  public void setUserSettings(RoomUserSettingsDto userSettings) {
    this.userSettings = userSettings;
  }

  /**
   * room name
   **/
  
  @ApiModelProperty(required = true, value = "room name")
  @JsonProperty("name")
  @NotNull
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  /**
   * room description
   **/
  
  @ApiModelProperty(required = true, value = "room description")
  @JsonProperty("description")
  @NotNull
  public String getDescription() {
    return description;
  }
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   **/
  
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("type")
  @NotNull
  public RoomTypeDto getType() {
    return type;
  }
  public void setType(RoomTypeDto type) {
    this.type = type;
  }

  /**
   * room identifier
   **/
  
  @ApiModelProperty(required = true, value = "room identifier")
  @JsonProperty("id")
  @NotNull
  public UUID getId() {
    return id;
  }
  public void setId(UUID id) {
    this.id = id;
  }

  /**
   * an hash that can be used to compose the room&#39;s link
   **/
  
  @ApiModelProperty(required = true, value = "an hash that can be used to compose the room's link")
  @JsonProperty("hash")
  @NotNull
  public String getHash() {
    return hash;
  }
  public void setHash(String hash) {
    this.hash = hash;
  }

  /**
   * creation date
   **/
  
  @ApiModelProperty(required = true, value = "creation date")
  @JsonProperty("createdAt")
  @NotNull
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  /**
   * update date
   **/
  
  @ApiModelProperty(required = true, value = "update date")
  @JsonProperty("updatedAt")
  @NotNull
  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  /**
   * password to room access
   **/
  
  @ApiModelProperty(value = "password to room access")
  @JsonProperty("password")
  public String getPassword() {
    return password;
  }
  public void setPassword(String password) {
    this.password = password;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RoomInfoDto roomInfo = (RoomInfoDto) o;
    return Objects.equals(members, roomInfo.members) &&
        Objects.equals(userSettings, roomInfo.userSettings) &&
        Objects.equals(name, roomInfo.name) &&
        Objects.equals(description, roomInfo.description) &&
        Objects.equals(type, roomInfo.type) &&
        Objects.equals(id, roomInfo.id) &&
        Objects.equals(hash, roomInfo.hash) &&
        Objects.equals(createdAt, roomInfo.createdAt) &&
        Objects.equals(updatedAt, roomInfo.updatedAt) &&
        Objects.equals(password, roomInfo.password);
  }

  @Override
  public int hashCode() {
    return Objects.hash(members, userSettings, name, description, type, id, hash, createdAt, updatedAt, password);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RoomInfoDto {\n");
    
    sb.append("    members: ").append(toIndentedString(members)).append("\n");
    sb.append("    userSettings: ").append(toIndentedString(userSettings)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    hash: ").append(toIndentedString(hash)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    updatedAt: ").append(toIndentedString(updatedAt)).append("\n");
    sb.append("    password: ").append(toIndentedString(password)).append("\n");
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

