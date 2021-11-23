package com.zextras.team.core.model;

import com.zextras.team.core.invoker.StringUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;
import java.util.ArrayList;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.zextras.team.core.model.RoomCreationFieldsAllOfDto;
import com.zextras.team.core.model.RoomEditableFieldsDto;
import com.zextras.team.core.model.RoomTypeDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.*;

/**
 * Room fields for its creation
 */
@ApiModel(description = "Room fields for its creation")
@Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen", date = "2021-11-23T16:37:04.902096+01:00[Europe/Rome]")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoomCreationFieldsDto {

  private List<String> membersIds = new ArrayList<>();
  private String name;
  private RoomTypeDto type;
  private String description;

  public static RoomCreationFieldsDto create() {
    return new RoomCreationFieldsDto();
  }

  /**
   * members identifiers list to be subscribed to the room
  **/
  @ApiModelProperty(required = true, value = "members identifiers list to be subscribed to the room")
  @JsonProperty("membersIds") @NotNull @Size(min = 2)
  public List<String> getMembersIds() {
    return membersIds;
  }

  public void setMembersIds(List<String> membersIds) {
    this.membersIds = membersIds;
  }

  public RoomCreationFieldsDto membersIds(List<String> membersIds) {
    this.membersIds = membersIds;
    return this;
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

  public RoomCreationFieldsDto name(String name) {
    this.name = name;
    return this;
  }

  @ApiModelProperty(required = true, value = "")
  @JsonProperty("type") @NotNull
  public RoomTypeDto getType() {
    return type;
  }

  public void setType(RoomTypeDto type) {
    this.type = type;
  }

  public RoomCreationFieldsDto type(RoomTypeDto type) {
    this.type = type;
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

  public RoomCreationFieldsDto description(String description) {
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
    RoomCreationFieldsDto roomCreationFields = (RoomCreationFieldsDto) o;
    return Objects.equals(this.membersIds, roomCreationFields.membersIds) &&
      Objects.equals(this.name, roomCreationFields.name) &&
      Objects.equals(this.type, roomCreationFields.type) &&
      Objects.equals(this.description, roomCreationFields.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(membersIds, name, type, description);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RoomCreationFieldsDto {\n");
    sb.append("  membersIds: ").append(StringUtil.toIndentedString(membersIds)).append("\n");
    sb.append("  name: ").append(StringUtil.toIndentedString(name)).append("\n");
    sb.append("  type: ").append(StringUtil.toIndentedString(type)).append("\n");
    sb.append("  description: ").append(StringUtil.toIndentedString(description)).append("\n");
    sb.append("}");
    return sb.toString();
  }
}
