package org.jminor.plugin.jackson.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

final class LocalDateSerializer extends StdSerializer<LocalDate> {

  LocalDateSerializer() {
    super(LocalDate.class);
  }

  @Override
  public void serialize(final LocalDate localDate, final JsonGenerator generator, final SerializerProvider provider) throws IOException {
    generator.writeString(localDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
  }
}