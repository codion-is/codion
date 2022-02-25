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

  AttributeConditionSerializer(EntityObjectMapper entityObjectMapper) {
    this.entityObjectMapper = entityObjectMapper;
  }

  void serialize(AttributeCondition<?> condition, JsonGenerator generator) throws IOException {
    generator.writeStartObject();
    generator.writeStringField("type", "attribute");
    generator.writeStringField("attribute", condition.getAttribute().getName());
    generator.writeStringField("operator", condition.getOperator().name());
    generator.writeFieldName("values");
    generator.writeStartArray();
    for (Object value : condition.getValues()) {
      entityObjectMapper.writeValue(generator, value);
    }
    generator.writeEndArray();
    generator.writeEndObject();
  }
}
