/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.domain;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

final class OffsetDateTimeSerializer extends StdSerializer<OffsetDateTime> {

  private static final long serialVersionUID = 1;

  OffsetDateTimeSerializer() {
    super(OffsetDateTime.class);
  }

  @Override
  public void serialize(OffsetDateTime dateTime, JsonGenerator generator, SerializerProvider provider) throws IOException {
    generator.writeString(dateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
  }
}