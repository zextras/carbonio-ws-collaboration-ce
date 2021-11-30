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
import com.zextras.chats.core.model.RoomCreationFieldsAllOfDto;
import com.zextras.chats.core.model.RoomEditableFieldsDto;
import com.zextras.chats.core.model.RoomTypeDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.*;

/**
 * Room fields for its creation
 */
@ApiModel(description = "Room fields for its creation")
@Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoomCreationFieldsDto {

  /**
   * members identifiers list to be subscribed to the room
  **/
  @ApiModelProperty(required = true, value = "members identifiers list to be subscribed to the room")
  @JsonProperty("membersIds") @NotNull @Size(min = 2)
  private List<String> membersIds = new ArrayList<>();

  /**
   * room name
  **/
  @ApiModelProperty(required = true, value = "room name")
  @JsonProperty("name") @NotNull
  private String name;

  /**
   * room description
  **/
  @ApiModelProperty(required = true, value = "room description")
  @JsonProperty("description") @NotNull
  private String description;

  @ApiModelProperty(required = true, value = "")
  @JsonProperty("type") @NotNull
  private RoomTypeDto type;

  public static RoomCreationFieldsDto create() {
    return new RoomCreationFieldsDto();
  }

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
      Objects.equals(this.description, roomCreationFields.description) &&
      Objects.equals(this.type, roomCreationFields.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(membersIds, name, description, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RoomCreationFieldsDto {\n");
    sb.append("  membersIds: ").append(StringUtil.toIndentedString(membersIds)).append("\n");
    sb.append("  name: ").append(StringUtil.toIndentedString(name)).append("\n");
    sb.append("  description: ").append(StringUtil.toIndentedString(description)).append("\n");
    sb.append("  type: ").append(StringUtil.toIndentedString(type)).append("\n");
    sb.append("}");
    return sb.toString();
  }
}
