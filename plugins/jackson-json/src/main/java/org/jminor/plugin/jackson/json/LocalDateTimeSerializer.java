package org.jminor.plugin.jackson.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

final class LocalDateTimeSerializer extends StdSerializer<LocalDateTime> {

  LocalDateTimeSerializer() {
    super(LocalDateTime.class);
  }

  @Override
  public void serialize(final LocalDateTime dateTime, final JsonGenerator generator, final SerializerProvider provider) throws IOException {
    generator.writeString(dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
  }
}