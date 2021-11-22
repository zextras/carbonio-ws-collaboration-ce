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
 * Room fields that can be updated
 */
@ApiModel(description = "Room fields that can be updated")
@Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen", date = "2021-11-22T10:48:21.557692+01:00[Europe/Rome]")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoomEditableFieldsDto {

  private String name;
  private String description;

  public static RoomEditableFieldsDto create() {
    return new RoomEditableFieldsDto();
  }

  /**
   * room name
  **/
  @ApiModelProperty(required = true, value = "room name")
  @JsonProperty("name") @NotNull
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public RoomEditableFieldsDto name(String name) {
    this.name = name;
    return this;
  }

  /**
   * room description
  **/
  @ApiModelProperty(value = "room description")
  @JsonProperty("description") 
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public RoomEditableFieldsDto description(String description) {
    this.description = description;
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
    RoomEditableFieldsDto roomEditableFields = (RoomEditableFieldsDto) o;
    return Objects.equals(this.name, roomEditableFields.name) &&
      Objects.equals(this.description, roomEditableFields.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, description);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RoomEditableFieldsDto {\n");
    sb.append("  name: ").append(StringUtil.toIndentedString(name)).append("\n");
    sb.append("  description: ").append(StringUtil.toIndentedString(description)).append("\n");
    sb.append("}");
    return sb.toString();
  }
}
