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
 * Hash object
 */
@ApiModel(description = "Hash object")
@Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen")
@JsonIgnoreProperties(ignoreUnknown = true)
public class HashDto {

  /**
   * hash
  **/
  @ApiModelProperty(required = true, value = "hash")
  @JsonProperty("hash") @NotNull
  private String hash;

  public static HashDto create() {
    return new HashDto();
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public HashDto hash(String hash) {
    this.hash = hash;
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
    HashDto hash = (HashDto) o;
    return Objects.equals(this.hash, hash.hash);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hash);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class HashDto {\n");
    sb.append("  hash: ").append(StringUtil.toIndentedString(hash)).append("\n");
    sb.append("}");
    return sb.toString();
  }
}
