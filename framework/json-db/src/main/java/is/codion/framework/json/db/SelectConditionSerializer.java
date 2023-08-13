/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Objects;

final class SelectConditionSerializer extends StdSerializer<SelectCondition> {

  private static final long serialVersionUID = 1;

  private final CriteriaSerializer criteriaSerializer;
  private final Entities entities;

  SelectConditionSerializer(EntityObjectMapper entityObjectMapper) {
    super(SelectCondition.class);
    this.criteriaSerializer = new CriteriaSerializer(entityObjectMapper);
    this.entities = entityObjectMapper.entities();
  }

  @Override
  public void serialize(SelectCondition condition, JsonGenerator generator,
                        SerializerProvider provider) throws IOException {
    generator.writeStartObject();
    generator.writeStringField("entityType", condition.criteria().entityType().name());
    generator.writeFieldName("criteria");
    criteriaSerializer.serialize(condition.criteria(), generator);
    generator.writeFieldName("orderBy");
    OrderBy orderBy = condition.orderBy().orElse(null);
    if (orderBy == null) {
      generator.writeNull();
    }
    else {
      generator.writeStartArray();
      for (OrderBy.OrderByColumn orderByColumn : orderBy.orderByColumns()) {
        generator.writeString(orderByColumn.column().name() +
                ":" + (orderByColumn.isAscending() ? "asc" : "desc") +
                ":" + orderByColumn.nullOrder().name());
      }
      generator.writeEndArray();
    }
    generator.writeObjectField("limit", condition.limit());
    generator.writeObjectField("offset", condition.offset());
    generator.writeObjectField("forUpdate", condition.forUpdate());
    generator.writeObjectField("queryTimeout", condition.queryTimeout());
    Integer conditionFetchDepth = condition.fetchDepth().orElse(null);
    generator.writeObjectField("fetchDepth", conditionFetchDepth);
    generator.writeFieldName("fkFetchDepth");
    generator.writeStartObject();
    for (ForeignKey foreignKey : entities.definition(condition.criteria().entityType()).foreignKeys()) {
      Integer fkFetchDepth = condition.fetchDepth(foreignKey).orElse(null);
      if (!Objects.equals(fkFetchDepth, conditionFetchDepth)) {
        generator.writeObjectField(foreignKey.name(), fkFetchDepth);
      }
    }
    generator.writeEndObject();
    generator.writeFieldName("attributes");
    generator.writeStartArray();
    for (Attribute<?> attribute : condition.attributes()) {
      generator.writeString(attribute.name());
    }
    generator.writeEndArray();
    generator.writeEndObject();
  }
}
