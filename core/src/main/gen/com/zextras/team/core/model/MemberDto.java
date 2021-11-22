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
 * Information about a user&#39;s role in the room
 */
@ApiModel(description = "Information about a user's role in the room")
@Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen", date = "2021-11-22T10:48:21.557692+01:00[Europe/Rome]")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MemberDto {

  private UUID userId;
  private Boolean isOwner = false;
  private Boolean isTemporary = false;
  private Boolean isExternal = false;

  public static MemberDto create() {
    return new MemberDto();
  }

  /**
   * user identifier
  **/
  @ApiModelProperty(required = true, value = "user identifier")
  @JsonProperty("userId") @NotNull
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

  /**
   * indicates whether it is the owner
  **/
  @ApiModelProperty(required = true, value = "indicates whether it is the owner")
  @JsonProperty("isOwner") @NotNull
  public Boolean getIsOwner() {
    return isOwner;
  }

  public void setIsOwner(Boolean isOwner) {
    this.isOwner = isOwner;
  }

  public MemberDto isOwner(Boolean isOwner) {
    this.isOwner = isOwner;
    return this;
  }

  /**
   * indicates whether it is temporary
  **/
  @ApiModelProperty(required = true, value = "indicates whether it is temporary")
  @JsonProperty("isTemporary") @NotNull
  public Boolean getIsTemporary() {
    return isTemporary;
  }

  public void setIsTemporary(Boolean isTemporary) {
    this.isTemporary = isTemporary;
  }

  public MemberDto isTemporary(Boolean isTemporary) {
    this.isTemporary = isTemporary;
    return this;
  }

  /**
   * indicates whether it is enternal user
  **/
  @ApiModelProperty(required = true, value = "indicates whether it is enternal user")
  @JsonProperty("isExternal") @NotNull
  public Boolean getIsExternal() {
    return isExternal;
  }

  public void setIsExternal(Boolean isExternal) {
    this.isExternal = isExternal;
  }

  public MemberDto isExternal(Boolean isExternal) {
    this.isExternal = isExternal;
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
      Objects.equals(this.isOwner, member.isOwner) &&
      Objects.equals(this.isTemporary, member.isTemporary) &&
      Objects.equals(this.isExternal, member.isExternal);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, isOwner, isTemporary, isExternal);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MemberDto {\n");
    sb.append("  userId: ").append(StringUtil.toIndentedString(userId)).append("\n");
    sb.append("  isOwner: ").append(StringUtil.toIndentedString(isOwner)).append("\n");
    sb.append("  isTemporary: ").append(StringUtil.toIndentedString(isTemporary)).append("\n");
    sb.append("  isExternal: ").append(StringUtil.toIndentedString(isExternal)).append("\n");
    sb.append("}");
    return sb.toString();
  }
}
