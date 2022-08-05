/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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

  CustomConditionSerializer(EntityObjectMapper entityObjectMapper) {
    this.entityObjectMapper = entityObjectMapper;
  }

  void serialize(CustomCondition condition, JsonGenerator generator) throws IOException {
    generator.writeStartObject();
    generator.writeStringField("type", "custom");
    generator.writeStringField("conditionTypeName", condition.conditionType().name());
    generator.writeFieldName("attributes");
    entityObjectMapper.writeValue(generator, condition.attributes().stream()
            .map(Attribute::name)
            .collect(toList()));
    generator.writeFieldName("values");
    entityObjectMapper.writeValue(generator, condition.values());
    generator.writeEndObject();
  }
}
