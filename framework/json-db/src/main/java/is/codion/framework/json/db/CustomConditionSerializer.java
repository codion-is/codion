/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.condition.CustomCondition;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.io.Serializable;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class CustomConditionSerializer implements Serializable {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;

  CustomConditionSerializer(EntityObjectMapper entityObjectMapper) {
    this.entityObjectMapper = requireNonNull(entityObjectMapper);
  }

  void serialize(CustomCondition condition, JsonGenerator generator) throws IOException {
    generator.writeStartObject();
    generator.writeStringField("type", "custom");
    generator.writeStringField("conditionType", condition.conditionType().name());
    generator.writeFieldName("columns");
    entityObjectMapper.writeValue(generator, condition.columns().stream()
            .map(Attribute::name)
            .collect(toList()));
    generator.writeFieldName("values");
    entityObjectMapper.writeValue(generator, condition.values());
    generator.writeEndObject();
  }
}
