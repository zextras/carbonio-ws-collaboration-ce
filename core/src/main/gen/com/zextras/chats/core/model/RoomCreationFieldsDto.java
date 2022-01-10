package com.zextras.chats.core.model;

import java.util.Objects;
import java.util.ArrayList;
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
import io.swagger.annotations.*;

@ApiModel(description="Room fields for its creation")@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen")
public class RoomCreationFieldsDto   {
  
  private List<String> membersIds = new ArrayList<>();
  private String name;
  private String description;
  private RoomTypeDto type;

  /**
   * members identifiers list to be subscribed to the room
   **/
  
  @ApiModelProperty(required = true, value = "members identifiers list to be subscribed to the room")
  @JsonProperty("membersIds")
  @NotNull
 @Size(min=2)  public List<String> getMembersIds() {
    return membersIds;
  }
  public void setMembersIds(List<String> membersIds) {
    this.membersIds = membersIds;
  }

  /**
   * room name
   **/
  
  @ApiModelProperty(required = true, value = "room name")
  @JsonProperty("name")
  @NotNull
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  /**
   * room description
   **/
  
  @ApiModelProperty(required = true, value = "room description")
  @JsonProperty("description")
  @NotNull
  public String getDescription() {
    return description;
  }
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   **/
  
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("type")
  @NotNull
  public RoomTypeDto getType() {
    return type;
  }
  public void setType(RoomTypeDto type) {
    this.type = type;
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
    return Objects.equals(membersIds, roomCreationFields.membersIds) &&
        Objects.equals(name, roomCreationFields.name) &&
        Objects.equals(description, roomCreationFields.description) &&
        Objects.equals(type, roomCreationFields.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(membersIds, name, description, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RoomCreationFieldsDto {\n");
    
    sb.append("    membersIds: ").append(toIndentedString(membersIds)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
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

