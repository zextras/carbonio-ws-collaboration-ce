package com.zextras.carbonio.chats.core.model;

import java.util.Objects;
import java.util.ArrayList;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.zextras.carbonio.chats.core.model.HealthDependencyTypeDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.*;
import io.swagger.annotations.*;

@ApiModel(description="Health status of a service dependency")@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen")
public class DependencyHealthDto   {
  
  private HealthDependencyTypeDto name;
  private Boolean isHealthy;

  /**
   **/
  
  @ApiModelProperty(value = "")
  @JsonProperty("name")
  public HealthDependencyTypeDto getName() {
    return name;
  }
  public void setName(HealthDependencyTypeDto name) {
    this.name = name;
  }

  /**
   * whether the dependency is available and operative
   **/
  
  @ApiModelProperty(value = "whether the dependency is available and operative")
  @JsonProperty("isHealthy")
  public Boolean isIsHealthy() {
    return isHealthy;
  }
  public void setIsHealthy(Boolean isHealthy) {
    this.isHealthy = isHealthy;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DependencyHealthDto dependencyHealth = (DependencyHealthDto) o;
    return Objects.equals(name, dependencyHealth.name) &&
        Objects.equals(isHealthy, dependencyHealth.isHealthy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, isHealthy);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DependencyHealthDto {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    isHealthy: ").append(toIndentedString(isHealthy)).append("\n");
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

