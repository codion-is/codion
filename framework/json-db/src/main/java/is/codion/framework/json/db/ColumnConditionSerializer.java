/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.domain.entity.attribute.ColumnCondition;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.io.Serializable;

import static java.util.Objects.requireNonNull;

final class ColumnConditionSerializer implements Serializable {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;

  ColumnConditionSerializer(EntityObjectMapper entityObjectMapper) {
    this.entityObjectMapper = requireNonNull(entityObjectMapper);
  }

  void serialize(ColumnCondition<?> condition, JsonGenerator generator) throws IOException {
    generator.writeStartObject();
    generator.writeStringField("type", "column");
    generator.writeStringField("column", condition.column().name());
    generator.writeStringField("operator", condition.operator().name());
    generator.writeBooleanField("caseSensitive", condition.caseSensitive());
    generator.writeFieldName("values");
    generator.writeStartArray();
    for (Object value : condition.values()) {
      entityObjectMapper.writeValue(generator, value);
    }
    generator.writeEndArray();
    generator.writeEndObject();
  }
}
