package org.jminor.plugin.jackson.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

final class LocalTimeSerializer extends StdSerializer<LocalTime> {

  LocalTimeSerializer() {
    super(LocalTime.class);
  }

  @Override
  public void serialize(final LocalTime localTime, final JsonGenerator generator, final SerializerProvider provider) throws IOException {
    generator.writeString(localTime.format(DateTimeFormatter.ISO_LOCAL_TIME));
  }
}