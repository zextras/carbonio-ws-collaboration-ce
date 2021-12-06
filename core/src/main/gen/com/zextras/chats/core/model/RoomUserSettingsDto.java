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
 * Preferences that an user has set for a room
 */
@ApiModel(description = "Preferences that an user has set for a room")
@Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoomUserSettingsDto {

  /**
   * indicates whether the user has muted
  **/
  @ApiModelProperty(required = true, value = "indicates whether the user has muted")
  @JsonProperty("muted") @NotNull
  private Boolean muted;

  public static RoomUserSettingsDto create() {
    return new RoomUserSettingsDto();
  }

  public Boolean isMuted() {
    return muted;
  }

  public void setMuted(Boolean muted) {
    this.muted = muted;
  }

  public RoomUserSettingsDto muted(Boolean muted) {
    this.muted = muted;
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
    RoomUserSettingsDto roomUserSettings = (RoomUserSettingsDto) o;
    return Objects.equals(this.muted, roomUserSettings.muted);
  }

  @Override
  public int hashCode() {
    return Objects.hash(muted);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RoomUserSettingsDto {\n");
    sb.append("  muted: ").append(StringUtil.toIndentedString(muted)).append("\n");
    sb.append("}");
    return sb.toString();
  }
}
