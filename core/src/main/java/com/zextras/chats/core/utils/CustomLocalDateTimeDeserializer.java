package com.zextras.chats.core.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class CustomLocalDateTimeDeserializer  extends StdDeserializer<LocalDateTime> {

  protected CustomLocalDateTimeDeserializer() {
    this(null);
  }

  protected CustomLocalDateTimeDeserializer(Class<LocalDateTime> t) {
    super(t);
  }

  @Override
  public LocalDateTime deserialize(JsonParser jsonparser, DeserializationContext context)
    throws IOException {
    Long timestamp = jsonparser.getLongValue();
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
  }

}
