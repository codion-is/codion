/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.criteria.CustomCriteria;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.io.Serializable;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class CustomCriteriaSerializer implements Serializable {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;

  CustomCriteriaSerializer(EntityObjectMapper entityObjectMapper) {
    this.entityObjectMapper = requireNonNull(entityObjectMapper);
  }

  void serialize(CustomCriteria criteria, JsonGenerator generator) throws IOException {
    generator.writeStartObject();
    generator.writeStringField("type", "custom");
    generator.writeStringField("criteriaType", criteria.criteriaType().name());
    generator.writeFieldName("attributes");
    entityObjectMapper.writeValue(generator, criteria.attributes().stream()
            .map(Attribute::name)
            .collect(toList()));
    generator.writeFieldName("values");
    entityObjectMapper.writeValue(generator, criteria.values());
    generator.writeEndObject();
  }
}
