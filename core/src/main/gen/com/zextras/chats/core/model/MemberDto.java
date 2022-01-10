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

@ApiModel(description="Information about a user's role in the room")@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen")
public class MemberDto   {
  
  private UUID userId;
  private Boolean owner = false;
  private Boolean temporary = false;
  private Boolean external = false;

  /**
   * user identifier
   **/
  
  @ApiModelProperty(required = true, value = "user identifier")
  @JsonProperty("userId")
  @NotNull
  public UUID getUserId() {
    return userId;
  }
  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  /**
   * indicates whether it is the owner
   **/
  
  @ApiModelProperty(required = true, value = "indicates whether it is the owner")
  @JsonProperty("owner")
  @NotNull
  public Boolean isOwner() {
    return owner;
  }
  public void setOwner(Boolean owner) {
    this.owner = owner;
  }

  /**
   * indicates whether it is temporary
   **/
  
  @ApiModelProperty(required = true, value = "indicates whether it is temporary")
  @JsonProperty("temporary")
  @NotNull
  public Boolean isTemporary() {
    return temporary;
  }
  public void setTemporary(Boolean temporary) {
    this.temporary = temporary;
  }

  /**
   * indicates whether it is enternal user
   **/
  
  @ApiModelProperty(required = true, value = "indicates whether it is enternal user")
  @JsonProperty("external")
  @NotNull
  public Boolean isExternal() {
    return external;
  }
  public void setExternal(Boolean external) {
    this.external = external;
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
    return Objects.equals(userId, member.userId) &&
        Objects.equals(owner, member.owner) &&
        Objects.equals(temporary, member.temporary) &&
        Objects.equals(external, member.external);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, owner, temporary, external);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MemberDto {\n");
    
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    owner: ").append(toIndentedString(owner)).append("\n");
    sb.append("    temporary: ").append(toIndentedString(temporary)).append("\n");
    sb.append("    external: ").append(toIndentedString(external)).append("\n");
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

