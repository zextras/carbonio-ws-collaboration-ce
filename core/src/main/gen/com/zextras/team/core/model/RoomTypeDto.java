package com.zextras.team.core.model;

import com.zextras.team.core.invoker.StringUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;
import java.util.ArrayList;
import javax.annotation.Generated;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.constraints.*;
/**
* Room types managed
*/
@ApiModel(description="Room types managed")
@Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen", date = "2021-11-23T16:37:04.902096+01:00[Europe/Rome]")
public enum RoomTypeDto {
  CHANNEL("Channel"),
  TEMPORARY("Temporary"),
  GROUP("Group"),
  SPACE("Space"),
  ONETOONE("OneToOne"),
  USERROOM("UserRoom");

  private final Object value;

  RoomTypeDto(Object value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }
}

