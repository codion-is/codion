/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json.db;

import org.jminor.framework.db.condition.PropertyCondition;
import org.jminor.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.io.Serializable;

final class PropertyConditionSerializer implements Serializable {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;

  PropertyConditionSerializer(final EntityObjectMapper entityObjectMapper) {
    this.entityObjectMapper = entityObjectMapper;
  }

  void serialize(final PropertyCondition condition, final JsonGenerator generator) throws IOException {
    generator.writeStartObject();
    generator.writeObjectField("type", "property");
    generator.writeObjectField("propertyId", condition.getPropertyId());
    generator.writeObjectField("conditionType", condition.getConditionType().toString());
    generator.writeFieldName("values");
    generator.writeStartArray();
    for (final Object value : condition.getValues()) {
      entityObjectMapper.writeValue(generator, value);
    }
    generator.writeEndArray();
    generator.writeEndObject();
  }
}
