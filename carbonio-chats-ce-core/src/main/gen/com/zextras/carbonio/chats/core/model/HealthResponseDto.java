package com.zextras.carbonio.chats.core.model;

import java.util.Objects;
import java.util.ArrayList;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.zextras.carbonio.chats.core.model.DependencyHealthDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.*;
import io.swagger.annotations.*;

@ApiModel(description="Health status of the service and its dependencies")@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen")
public class HealthResponseDto   {
  
  private Boolean isLive;
  private Boolean isReady;
  private List<DependencyHealthDto> dependencies = new ArrayList<>();

  /**
   * describes if the service is alive
   **/
  
  @ApiModelProperty(value = "describes if the service is alive")
  @JsonProperty("isLive")
  public Boolean isIsLive() {
    return isLive;
  }
  public void setIsLive(Boolean isLive) {
    this.isLive = isLive;
  }

  /**
   * describes if the service is ready to accept requests
   **/
  
  @ApiModelProperty(value = "describes if the service is ready to accept requests")
  @JsonProperty("isReady")
  public Boolean isIsReady() {
    return isReady;
  }
  public void setIsReady(Boolean isReady) {
    this.isReady = isReady;
  }

  /**
   * health of this service dependencies
   **/
  
  @ApiModelProperty(value = "health of this service dependencies")
  @JsonProperty("dependencies")
  public List<DependencyHealthDto> getDependencies() {
    return dependencies;
  }
  public void setDependencies(List<DependencyHealthDto> dependencies) {
    this.dependencies = dependencies;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HealthResponseDto healthResponse = (HealthResponseDto) o;
    return Objects.equals(isLive, healthResponse.isLive) &&
        Objects.equals(isReady, healthResponse.isReady) &&
        Objects.equals(dependencies, healthResponse.dependencies);
  }

  @Override
  public int hashCode() {
    return Objects.hash(isLive, isReady, dependencies);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class HealthResponseDto {\n");
    
    sb.append("    isLive: ").append(toIndentedString(isLive)).append("\n");
    sb.append("    isReady: ").append(toIndentedString(isReady)).append("\n");
    sb.append("    dependencies: ").append(toIndentedString(dependencies)).append("\n");
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

