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
import java.time.LocalDateTime;
import java.util.UUID;
import javax.validation.constraints.*;

@Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen", date = "2021-11-22T10:48:21.557692+01:00[Europe/Rome]")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoomAllOfDto {

  private UUID id;
  private String hash;
  private UUID parentId;
  private String password;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public static RoomAllOfDto create() {
    return new RoomAllOfDto();
  }

  /**
   * room identifier
  **/
  @ApiModelProperty(value = "room identifier")
  @JsonProperty("id") 
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public RoomAllOfDto id(UUID id) {
    this.id = id;
    return this;
  }

  /**
   * an hash that can be used to compose the room&#39;s link
  **/
  @ApiModelProperty(value = "an hash that can be used to compose the room's link")
  @JsonProperty("hash") 
  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public RoomAllOfDto hash(String hash) {
    this.hash = hash;
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

  public RoomAllOfDto parentId(UUID parentId) {
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

  public RoomAllOfDto password(String password) {
    this.password = password;
    return this;
  }

  /**
   * creation date
  **/
  @ApiModelProperty(value = "creation date")
  @JsonProperty("createdAt") 
  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public RoomAllOfDto createdAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * update date
  **/
  @ApiModelProperty(value = "update date")
  @JsonProperty("updatedAt") 
  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public RoomAllOfDto updatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
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
    RoomAllOfDto roomAllOf = (RoomAllOfDto) o;
    return Objects.equals(this.id, roomAllOf.id) &&
      Objects.equals(this.hash, roomAllOf.hash) &&
      Objects.equals(this.parentId, roomAllOf.parentId) &&
      Objects.equals(this.password, roomAllOf.password) &&
      Objects.equals(this.createdAt, roomAllOf.createdAt) &&
      Objects.equals(this.updatedAt, roomAllOf.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, hash, parentId, password, createdAt, updatedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RoomAllOfDto {\n");
    sb.append("  id: ").append(StringUtil.toIndentedString(id)).append("\n");
    sb.append("  hash: ").append(StringUtil.toIndentedString(hash)).append("\n");
    sb.append("  parentId: ").append(StringUtil.toIndentedString(parentId)).append("\n");
    sb.append("  password: ").append(StringUtil.toIndentedString(password)).append("\n");
    sb.append("  createdAt: ").append(StringUtil.toIndentedString(createdAt)).append("\n");
    sb.append("  updatedAt: ").append(StringUtil.toIndentedString(updatedAt)).append("\n");
    sb.append("}");
    return sb.toString();
  }
}
