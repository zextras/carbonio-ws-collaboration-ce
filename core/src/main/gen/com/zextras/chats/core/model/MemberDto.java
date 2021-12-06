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
import java.util.UUID;
import javax.validation.constraints.*;

/**
 * Information about a user&#39;s role in the room
 */
@ApiModel(description = "Information about a user's role in the room")
@Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MemberDto {

  /**
   * user identifier
  **/
  @ApiModelProperty(required = true, value = "user identifier")
  @JsonProperty("userId") @NotNull
  private UUID userId;

  /**
   * indicates whether it is the owner
  **/
  @ApiModelProperty(required = true, value = "indicates whether it is the owner")
  @JsonProperty("owner") @NotNull
  private Boolean owner = false;

  /**
   * indicates whether it is temporary
  **/
  @ApiModelProperty(required = true, value = "indicates whether it is temporary")
  @JsonProperty("temporary") @NotNull
  private Boolean temporary = false;

  /**
   * indicates whether it is enternal user
  **/
  @ApiModelProperty(required = true, value = "indicates whether it is enternal user")
  @JsonProperty("external") @NotNull
  private Boolean external = false;

  public static MemberDto create() {
    return new MemberDto();
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public MemberDto userId(UUID userId) {
    this.userId = userId;
    return this;
  }

  public Boolean isOwner() {
    return owner;
  }

  public void setOwner(Boolean owner) {
    this.owner = owner;
  }

  public MemberDto owner(Boolean owner) {
    this.owner = owner;
    return this;
  }

  public Boolean isTemporary() {
    return temporary;
  }

  public void setTemporary(Boolean temporary) {
    this.temporary = temporary;
  }

  public MemberDto temporary(Boolean temporary) {
    this.temporary = temporary;
    return this;
  }

  public Boolean isExternal() {
    return external;
  }

  public void setExternal(Boolean external) {
    this.external = external;
  }

  public MemberDto external(Boolean external) {
    this.external = external;
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
    MemberDto member = (MemberDto) o;
    return Objects.equals(this.userId, member.userId) &&
      Objects.equals(this.owner, member.owner) &&
      Objects.equals(this.temporary, member.temporary) &&
      Objects.equals(this.external, member.external);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, owner, temporary, external);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MemberDto {\n");
    sb.append("  userId: ").append(StringUtil.toIndentedString(userId)).append("\n");
    sb.append("  owner: ").append(StringUtil.toIndentedString(owner)).append("\n");
    sb.append("  temporary: ").append(StringUtil.toIndentedString(temporary)).append("\n");
    sb.append("  external: ").append(StringUtil.toIndentedString(external)).append("\n");
    sb.append("}");
    return sb.toString();
  }
}
