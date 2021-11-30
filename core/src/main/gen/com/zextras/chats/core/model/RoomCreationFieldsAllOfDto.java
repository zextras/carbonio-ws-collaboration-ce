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
import com.zextras.chats.core.model.RoomTypeDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.*;

@Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoomCreationFieldsAllOfDto {

  @ApiModelProperty(value = "")
  @JsonProperty("type") 
  private RoomTypeDto type;

  public static RoomCreationFieldsAllOfDto create() {
    return new RoomCreationFieldsAllOfDto();
  }

  public RoomTypeDto getType() {
    return type;
  }

  public void setType(RoomTypeDto type) {
    this.type = type;
  }

  public RoomCreationFieldsAllOfDto type(RoomTypeDto type) {
    this.type = type;
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
    RoomCreationFieldsAllOfDto roomCreationFieldsAllOf = (RoomCreationFieldsAllOfDto) o;
    return Objects.equals(this.type, roomCreationFieldsAllOf.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RoomCreationFieldsAllOfDto {\n");
    sb.append("  type: ").append(StringUtil.toIndentedString(type)).append("\n");
    sb.append("}");
    return sb.toString();
  }
}
