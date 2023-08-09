/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.criteria.AttributeCriteria;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.io.Serializable;

import static java.util.Objects.requireNonNull;

final class AttributeCriteriaSerializer implements Serializable {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;

  AttributeCriteriaSerializer(EntityObjectMapper entityObjectMapper) {
    this.entityObjectMapper = requireNonNull(entityObjectMapper);
  }

  void serialize(AttributeCriteria<?> criteria, JsonGenerator generator) throws IOException {
    generator.writeStartObject();
    generator.writeStringField("type", "attribute");
    generator.writeStringField("attribute", criteria.attribute().name());
    generator.writeStringField("operator", criteria.operator().name());
    generator.writeBooleanField("caseSensitive", criteria.caseSensitive());
    generator.writeFieldName("values");
    generator.writeStartArray();
    for (Object value : criteria.values()) {
      entityObjectMapper.writeValue(generator, value);
    }
    generator.writeEndArray();
    generator.writeEndObject();
  }
}
