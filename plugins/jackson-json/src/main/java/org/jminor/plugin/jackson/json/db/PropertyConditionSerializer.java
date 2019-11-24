/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json.db;

import org.jminor.framework.db.condition.PropertyCondition;
import org.jminor.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

final class PropertyConditionSerializer {

  private final EntityObjectMapper entityObjectMapper;

  public PropertyConditionSerializer(final EntityObjectMapper entityObjectMapper) {
    this.entityObjectMapper = entityObjectMapper;
  }

  public void serialize(final PropertyCondition condition, final JsonGenerator generator, final SerializerProvider provider) throws IOException {
    generator.writeStartObject();
    generator.writeObjectField("type", "property");
    generator.writeObjectField("propertyId", condition.getPropertyId());
    generator.writeObjectField("conditionType", condition.getConditionType().toString());
    generator.writeFieldName("values");
    entityObjectMapper.writeValue(generator, condition.getValues());
    generator.writeEndObject();
  }
}
