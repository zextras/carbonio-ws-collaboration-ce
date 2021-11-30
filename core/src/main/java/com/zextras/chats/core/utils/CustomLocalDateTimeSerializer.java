package com.zextras.chats.core.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class CustomLocalDateTimeSerializer extends StdSerializer<LocalDateTime> {

  protected CustomLocalDateTimeSerializer(Class<LocalDateTime> type) {
    super(type);
  }

  protected CustomLocalDateTimeSerializer() {
    this(null);
  }

  @Override
  public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider sp)
    throws IOException {
    Long epoch = value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    gen.writeNumber(epoch);
  }
}