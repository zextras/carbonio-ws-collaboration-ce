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
import java.util.UUID;
import javax.validation.constraints.*;

/**
 * Identifier object
 */
@ApiModel(description = "Identifier object")
@Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen", date = "2021-11-23T16:37:04.902096+01:00[Europe/Rome]")
@JsonIgnoreProperties(ignoreUnknown = true)
public class IdDto {

  private UUID id;

  public static IdDto create() {
    return new IdDto();
  }

  /**
   * identifier
  **/
  @ApiModelProperty(required = true, value = "identifier")
  @JsonProperty("id") @NotNull
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public IdDto id(UUID id) {
    this.id = id;
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
    IdDto id = (IdDto) o;
    return Objects.equals(this.id, id.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IdDto {\n");
    sb.append("  id: ").append(StringUtil.toIndentedString(id)).append("\n");
    sb.append("}");
    return sb.toString();
  }
}
