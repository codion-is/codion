/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json.db;

import org.jminor.framework.db.condition.CustomCondition;
import org.jminor.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;

final class CustomConditionSerializer {

  private final EntityObjectMapper entityObjectMapper;

  public CustomConditionSerializer(final EntityObjectMapper entityObjectMapper) {
    this.entityObjectMapper = entityObjectMapper;
  }

  public void serialize(final CustomCondition condition, final JsonGenerator generator) throws IOException {
    generator.writeStartObject();
    generator.writeObjectField("type", "custom");
    generator.writeObjectField("conditionId", condition.getConditionId());
    generator.writeFieldName("propertyIds");
    entityObjectMapper.writeValue(generator, condition.getPropertyIds());
    generator.writeFieldName("values");
    entityObjectMapper.writeValue(generator, condition.getValues());
    generator.writeEndObject();
  }
}
