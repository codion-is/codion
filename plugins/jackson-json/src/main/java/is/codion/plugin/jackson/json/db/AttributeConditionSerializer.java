/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.framework.db.condition.AttributeCondition;
import is.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.io.Serializable;

final class AttributeConditionSerializer implements Serializable {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;

  AttributeConditionSerializer(final EntityObjectMapper entityObjectMapper) {
    this.entityObjectMapper = entityObjectMapper;
  }

  void serialize(final AttributeCondition<?> condition, final JsonGenerator generator) throws IOException {
    generator.writeStartObject();
    generator.writeStringField("type", "attribute");
    generator.writeStringField("attribute", condition.getAttribute().getName());
    generator.writeStringField("operator", condition.getOperator().name());
    generator.writeFieldName("values");
    generator.writeStartArray();
    for (final Object value : condition.getValues()) {
      entityObjectMapper.writeValue(generator, value);
    }
    generator.writeEndArray();
    generator.writeEndObject();
  }
}
