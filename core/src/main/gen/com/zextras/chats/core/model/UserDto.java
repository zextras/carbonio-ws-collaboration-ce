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
import javax.validation.constraints.*;

/**
 * User data
 */
@ApiModel(description = "User data")
@Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen")
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDto {

  /**
   * user&#39;s email
  **/
  @ApiModelProperty(required = true, value = "user's email")
  @JsonProperty("email") @NotNull
  private String email;

  /**
   * user&#39;s name
  **/
  @ApiModelProperty(required = true, value = "user's name")
  @JsonProperty("name") @NotNull
  private String name;

  public static UserDto create() {
    return new UserDto();
  }

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
