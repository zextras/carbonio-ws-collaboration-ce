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
import javax.validation.constraints.*;

/**
 * User data
 */
@ApiModel(description = "User data")
@Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen", date = "2021-11-23T16:37:04.902096+01:00[Europe/Rome]")
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDto {

  private String email;
  private String name;

  public static UserDto create() {
    return new UserDto();
  }

  /**
   * user&#39;s email
  **/
  @ApiModelProperty(required = true, value = "user's email")
  @JsonProperty("email") @NotNull
  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public UserDto email(String email) {
    this.email = email;
    return this;
  }

  /**
   * user&#39;s name
  **/
  @ApiModelProperty(required = true, value = "user's name")
  @JsonProperty("name") @NotNull
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public UserDto name(String name) {
    this.name = name;
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
    UserDto user = (UserDto) o;
    return Objects.equals(this.email, user.email) &&
      Objects.equals(this.name, user.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(email, name);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserDto {\n");
    sb.append("  email: ").append(StringUtil.toIndentedString(email)).append("\n");
    sb.append("  name: ").append(StringUtil.toIndentedString(name)).append("\n");
    sb.append("}");
    return sb.toString();
  }
}
