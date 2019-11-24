/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json.db;

import org.jminor.framework.db.condition.PropertyCondition;
import org.jminor.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

final class PropertyConditionSerializer extends StdSerializer<PropertyCondition> {

  private final EntityObjectMapper entityObjectMapper;

  public PropertyConditionSerializer(final EntityObjectMapper entityObjectMapper) {
    super(PropertyCondition.class);
    this.entityObjectMapper = entityObjectMapper;
  }

  @Override
  public void serialize(final PropertyCondition condition, final JsonGenerator generator, final SerializerProvider provider) throws IOException {
    generator.writeStartObject();
    generator.writeObjectField("propertyId", condition.getPropertyId());
    generator.writeObjectField("conditionType", condition.getConditionType());
    generator.writeFieldName("values");
    entityObjectMapper.writeValue(generator, condition.getValues());
    generator.writeEndObject();
  }
}
