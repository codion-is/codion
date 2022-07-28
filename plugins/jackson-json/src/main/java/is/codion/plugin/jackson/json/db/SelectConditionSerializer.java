/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Objects;

final class SelectConditionSerializer extends StdSerializer<SelectCondition> {

  private static final long serialVersionUID = 1;

  private final ConditionSerializer conditionSerializer;
  private final Entities entities;

  SelectConditionSerializer(EntityObjectMapper entityObjectMapper) {
    super(SelectCondition.class);
    this.conditionSerializer = new ConditionSerializer(entityObjectMapper);
    this.entities = entityObjectMapper.getEntities();
  }

  @Override
  public void serialize(SelectCondition condition, JsonGenerator generator,
                        SerializerProvider provider) throws IOException {
    generator.writeStartObject();
    generator.writeStringField("entityType", condition.getEntityType().getName());
    generator.writeFieldName("condition");
    conditionSerializer.serialize(condition.getCondition(), generator);
    generator.writeFieldName("orderBy");
    OrderBy orderBy = condition.getOrderBy().orElse(null);
    if (orderBy == null) {
      generator.writeNull();
    }
    else {
      generator.writeStartArray();
      for (OrderBy.OrderByAttribute attribute : orderBy.getOrderByAttributes()) {
        generator.writeString(attribute.getAttribute().getName() + ":" + (attribute.isAscending() ? "asc" : "desc"));
      }
      generator.writeEndArray();
    }
    generator.writeObjectField("limit", condition.getLimit());
    generator.writeObjectField("offset", condition.getOffset());
    generator.writeObjectField("forUpdate", condition.isForUpdate());
    generator.writeObjectField("queryTimeout", condition.getQueryTimeout());
    Integer conditionFetchDepth = condition.getFetchDepth().orElse(null);
    generator.writeObjectField("fetchDepth", conditionFetchDepth);
    generator.writeFieldName("fkFetchDepth");
    generator.writeStartObject();
    for (ForeignKey foreignKey : entities.getDefinition(condition.getEntityType()).getForeignKeys()) {
      Integer fkFetchDepth = condition.getFetchDepth(foreignKey).orElse(null);
      if (!Objects.equals(fkFetchDepth, conditionFetchDepth)) {
        generator.writeObjectField(foreignKey.getName(), fkFetchDepth);
      }
    }
    generator.writeEndObject();
    generator.writeFieldName("selectAttributes");
    generator.writeStartArray();
    for (Attribute<?> attribute : condition.getSelectAttributes()) {
      generator.writeString(attribute.getName());
    }
    generator.writeEndArray();
    generator.writeEndObject();
  }
}
