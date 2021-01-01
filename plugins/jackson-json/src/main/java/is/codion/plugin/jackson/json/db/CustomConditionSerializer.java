/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.framework.db.condition.CustomCondition;
import is.codion.framework.domain.entity.Attribute;
import is.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.io.Serializable;

import static java.util.stream.Collectors.toList;

final class CustomConditionSerializer implements Serializable {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;

  CustomConditionSerializer(final EntityObjectMapper entityObjectMapper) {
    this.entityObjectMapper = entityObjectMapper;
  }

  void serialize(final CustomCondition condition, final JsonGenerator generator) throws IOException {
    generator.writeStartObject();
    generator.writeStringField("type", "custom");
    generator.writeStringField("conditionTypeName", condition.getConditionType().getName());
    generator.writeFieldName("attributes");
    entityObjectMapper.writeValue(generator, condition.getAttributes().stream().map(Attribute::getName).collect(toList()));
    generator.writeFieldName("values");
    entityObjectMapper.writeValue(generator, condition.getValues());
    generator.writeEndObject();
  }
}
