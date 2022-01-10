package com.zextras.chats.core.model;

import java.util.Objects;
import java.util.ArrayList;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.*;
import io.swagger.annotations.*;

@ApiModel(description="Preferences that an user has set for a room")@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen")
public class RoomUserSettingsDto   {
  
  private Boolean muted;

  /**
   * indicates whether the user has muted
   **/
  
  @ApiModelProperty(required = true, value = "indicates whether the user has muted")
  @JsonProperty("muted")
  @NotNull
  public Boolean isMuted() {
    return muted;
  }
  public void setMuted(Boolean muted) {
    this.muted = muted;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RoomUserSettingsDto roomUserSettings = (RoomUserSettingsDto) o;
    return Objects.equals(muted, roomUserSettings.muted);
  }

  @Override
  public int hashCode() {
    return Objects.hash(muted);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RoomUserSettingsDto {\n");
    
    sb.append("    muted: ").append(toIndentedString(muted)).append("\n");
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

