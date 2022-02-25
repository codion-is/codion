/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.domain;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.math.BigDecimal;

final class BigDecimalSerializer extends StdSerializer<BigDecimal> {

  private static final long serialVersionUID = 1;

  BigDecimalSerializer() {
    super(BigDecimal.class);
  }

  @Override
  public void serialize(BigDecimal value, JsonGenerator generator, SerializerProvider provider) throws IOException {
    generator.writeString(value.toString());
  }
}
