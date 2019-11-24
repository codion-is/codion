package org.jminor.plugin.jackson.json.domain;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.math.BigDecimal;

final class BigDecimalSerializer extends StdSerializer<BigDecimal> {

  BigDecimalSerializer() {
    super(BigDecimal.class);
  }

  @Override
  public void serialize(final BigDecimal value, final JsonGenerator generator, final SerializerProvider provider) throws IOException {
    generator.writeString(value.toString());
  }
}
