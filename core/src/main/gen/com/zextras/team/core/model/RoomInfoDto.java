package com.zextras.team.core.model;

import com.zextras.team.core.invoker.StringUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;
import java.util.ArrayList;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.zextras.team.core.model.MemberDto;
import com.zextras.team.core.model.RoomDto;
import com.zextras.team.core.model.RoomTypeDto;
import com.zextras.team.core.model.RoomUserSettingsDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.validation.constraints.*;

/**
 * Room data with members list and current user settings
 */
@ApiModel(description = "Room data with members list and current user settings")
@Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen", date = "2021-11-23T16:37:04.902096+01:00[Europe/Rome]")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoomInfoDto {

  private List<MemberDto> members = new ArrayList<>();
  private RoomUserSettingsDto userSettings;
  private String name;
  private RoomTypeDto type;
  private UUID id;
  private String hash;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String description;
  private UUID parentId;
  private String password;

  public static RoomInfoDto create() {
    return new RoomInfoDto();
  }

  /**
   * list of users subscribed to the room
  **/
  @ApiModelProperty(required = true, value = "list of users subscribed to the room")
  @JsonProperty("members") @NotNull
  public List<MemberDto> getMembers() {
    return members;
  }

  public void setMembers(List<MemberDto> members) {
    this.members = members;
  }

  public RoomInfoDto members(List<MemberDto> members) {
    this.members = members;
    return this;
  }

  @ApiModelProperty(required = true, value = "")
  @JsonProperty("userSettings") @NotNull
  public RoomUserSettingsDto getUserSettings() {
    return userSettings;
  }

  public void setUserSettings(RoomUserSettingsDto userSettings) {
    this.userSettings = userSettings;
  }

  public RoomInfoDto userSettings(RoomUserSettingsDto userSettings) {
    this.userSettings = userSettings;
    return this;
  }

  /**
   * room name
  **/
  @ApiModelProperty(required = true, value = "room name")
  @JsonProperty("name") @NotNull
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public RoomInfoDto name(String name) {
    this.name = name;
    return this;
  }

  @ApiModelProperty(required = true, value = "")
  @JsonProperty("type") @NotNull
  public RoomTypeDto getType() {
    return type;
  }

  public void setType(RoomTypeDto type) {
    this.type = type;
  }

  public RoomInfoDto type(RoomTypeDto type) {
    this.type = type;
    return this;
  }

  /**
   * room identifier
  **/
  @ApiModelProperty(required = true, value = "room identifier")
  @JsonProperty("id") @NotNull
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public RoomInfoDto id(UUID id) {
    this.id = id;
    return this;
  }

  /**
   * an hash that can be used to compose the room&#39;s link
  **/
  @ApiModelProperty(required = true, value = "an hash that can be used to compose the room's link")
  @JsonProperty("hash") @NotNull
  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public RoomInfoDto hash(String hash) {
    this.hash = hash;
    return this;
  }

  /**
   * creation date
  **/
  @ApiModelProperty(required = true, value = "creation date")
  @JsonProperty("createdAt") @NotNull
  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public RoomInfoDto createdAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * update date
  **/
  @ApiModelProperty(required = true, value = "update date")
  @JsonProperty("updatedAt") @NotNull
  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public RoomInfoDto updatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  /**
   * room description
  **/
  @ApiModelProperty(value = "room description")
  @JsonProperty("description") 
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public RoomInfoDto description(String description) {
    this.description = description;
    return this;
  }

  /**
   * the id of an eventual parent room
  **/
  @ApiModelProperty(value = "the id of an eventual parent room")
  @JsonProperty("parentId") 
  public UUID getParentId() {
    return parentId;
  }

  public void setParentId(UUID parentId) {
    this.parentId = parentId;
  }

  public RoomInfoDto parentId(UUID parentId) {
    this.parentId = parentId;
    return this;
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

  public RoomInfoDto password(String password) {
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
    RoomInfoDto roomInfo = (RoomInfoDto) o;
    return Objects.equals(this.members, roomInfo.members) &&
      Objects.equals(this.userSettings, roomInfo.userSettings) &&
      Objects.equals(this.name, roomInfo.name) &&
      Objects.equals(this.type, roomInfo.type) &&
      Objects.equals(this.id, roomInfo.id) &&
      Objects.equals(this.hash, roomInfo.hash) &&
      Objects.equals(this.createdAt, roomInfo.createdAt) &&
      Objects.equals(this.updatedAt, roomInfo.updatedAt) &&
      Objects.equals(this.description, roomInfo.description) &&
      Objects.equals(this.parentId, roomInfo.parentId) &&
      Objects.equals(this.password, roomInfo.password);
  }

  @Override
  public int hashCode() {
    return Objects.hash(members, userSettings, name, type, id, hash, createdAt, updatedAt, description, parentId, password);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RoomInfoDto {\n");
    sb.append("  members: ").append(StringUtil.toIndentedString(members)).append("\n");
    sb.append("  userSettings: ").append(StringUtil.toIndentedString(userSettings)).append("\n");
    sb.append("  name: ").append(StringUtil.toIndentedString(name)).append("\n");
    sb.append("  type: ").append(StringUtil.toIndentedString(type)).append("\n");
    sb.append("  id: ").append(StringUtil.toIndentedString(id)).append("\n");
    sb.append("  hash: ").append(StringUtil.toIndentedString(hash)).append("\n");
    sb.append("  createdAt: ").append(StringUtil.toIndentedString(createdAt)).append("\n");
    sb.append("  updatedAt: ").append(StringUtil.toIndentedString(updatedAt)).append("\n");
    sb.append("  description: ").append(StringUtil.toIndentedString(description)).append("\n");
    sb.append("  parentId: ").append(StringUtil.toIndentedString(parentId)).append("\n");
    sb.append("  password: ").append(StringUtil.toIndentedString(password)).append("\n");
    sb.append("}");
    return sb.toString();
  }
}
