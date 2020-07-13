/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

final class SelectConditionSerializer extends StdSerializer<SelectCondition> {

  private static final long serialVersionUID = 1;

  private final ConditionSerializer conditionSerializer;
  private final Entities entities;

  SelectConditionSerializer(final EntityObjectMapper entityObjectMapper) {
    super(SelectCondition.class);
    this.conditionSerializer = new ConditionSerializer(entityObjectMapper);
    this.entities = entityObjectMapper.getEntities();
  }

  @Override
  public void serialize(final SelectCondition condition, final JsonGenerator generator,
                        final SerializerProvider provider) throws IOException {
    generator.writeStartObject();
    generator.writeStringField("entityType", condition.getEntityType().getName());
    generator.writeFieldName("condition");
    conditionSerializer.serialize(condition.getCondition(), generator);
    generator.writeFieldName("orderBy");
    if (condition.getOrderBy() == null) {
      generator.writeNull();
    }
    else {
      generator.writeStartArray();
      for (final OrderBy.OrderByAttribute attribute : condition.getOrderBy().getOrderByAttributes()) {
        generator.writeString(attribute.getAttribute().getName() + ":" + (attribute.isAscending() ? "asc" : "desc"));
      }
      generator.writeEndArray();
    }
    generator.writeObjectField("limit", condition.getLimit());
    generator.writeObjectField("offset", condition.getOffset());
    generator.writeObjectField("forUpdate", condition.isForUpdate());
    generator.writeObjectField("fetchCount", condition.getFetchCount());
    generator.writeObjectField("fetchDepth", condition.getFetchDepth());
    generator.writeFieldName("fkFetchDepth");
    generator.writeStartObject();
    for (final ForeignKeyProperty property : entities.getDefinition(condition.getEntityType()).getForeignKeyProperties()) {
      final Attribute<Entity> attribute = property.getAttribute();
      final Integer fkFetchDepth = condition.getFetchDepth(attribute);
      if (fkFetchDepth != condition.getFetchDepth()) {
        generator.writeObjectField(attribute.getName(), fkFetchDepth);
      }
    }
    generator.writeEndObject();
    generator.writeFieldName("selectAttributes");
    generator.writeStartArray();
    for (final Attribute<?> attribute : condition.getSelectAttributes()) {
      generator.writeString(attribute.getName());
    }
    generator.writeEndArray();
    generator.writeEndObject();
  }
}
