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
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.validation.constraints.*;

@Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoomAllOfDto {

  /**
   * room identifier
  **/
  @ApiModelProperty(value = "room identifier")
  @JsonProperty("id") 
  private UUID id;

  /**
   * an hash that can be used to compose the room&#39;s link
  **/
  @ApiModelProperty(value = "an hash that can be used to compose the room's link")
  @JsonProperty("hash") 
  private String hash;

  /**
   * password to room access
  **/
  @ApiModelProperty(value = "password to room access")
  @JsonProperty("password") 
  private String password;

  /**
   * creation date
  **/
  @ApiModelProperty(value = "creation date")
  @JsonProperty("createdAt") 
  private OffsetDateTime createdAt;

  /**
   * update date
  **/
  @ApiModelProperty(value = "update date")
  @JsonProperty("updatedAt") 
  private OffsetDateTime updatedAt;

  public static RoomAllOfDto create() {
    return new RoomAllOfDto();
  }

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

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public RoomAllOfDto createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public RoomAllOfDto updatedAt(OffsetDateTime updatedAt) {
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
      Objects.equals(this.password, roomAllOf.password) &&
      Objects.equals(this.createdAt, roomAllOf.createdAt) &&
      Objects.equals(this.updatedAt, roomAllOf.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, hash, password, createdAt, updatedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RoomAllOfDto {\n");
    sb.append("  id: ").append(StringUtil.toIndentedString(id)).append("\n");
    sb.append("  hash: ").append(StringUtil.toIndentedString(hash)).append("\n");
    sb.append("  password: ").append(StringUtil.toIndentedString(password)).append("\n");
    sb.append("  createdAt: ").append(StringUtil.toIndentedString(createdAt)).append("\n");
    sb.append("  updatedAt: ").append(StringUtil.toIndentedString(updatedAt)).append("\n");
    sb.append("}");
    return sb.toString();
  }
}
