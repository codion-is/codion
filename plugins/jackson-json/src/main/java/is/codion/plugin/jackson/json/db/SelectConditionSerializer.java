/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

final class SelectConditionSerializer extends StdSerializer<SelectCondition> {

  private static final long serialVersionUID = 1;

  private final ConditionSerializer conditionSerializer;

  SelectConditionSerializer(final EntityObjectMapper entityObjectMapper) {
    super(SelectCondition.class);
    this.conditionSerializer = new ConditionSerializer(entityObjectMapper);
  }

  @Override
  public void serialize(final SelectCondition condition, final JsonGenerator generator,
                        final SerializerProvider provider) throws IOException {
    generator.writeStartObject();
    generator.writeStringField("entityType", condition.getEntityType().getName());
    generator.writeFieldName("condition");
    conditionSerializer.serialize(condition.getCondition(), generator);
    generator.writeFieldName("orderBy");
    generator.writeStartArray();
    for (final OrderBy.OrderByAttribute attribute : condition.getOrderBy().getOrderByAttributes()) {
      generator.writeString(attribute.getAttribute().getName() + ":" + (attribute.isAscending() ? "asc" : "desc"));
    }
    generator.writeEndArray();
    generator.writeFieldName("limit");
    generator.writeObject(condition.getLimit());
    generator.writeFieldName("offset");
    generator.writeObject(condition.getOffset());
    generator.writeFieldName("forUpdate");
    generator.writeObject(condition.isForUpdate());
    generator.writeFieldName("fetchCount");
    generator.writeObject(condition.getFetchCount());
    generator.writeFieldName("selectAttributes");
    generator.writeStartArray();
    for (final Attribute<?> attribute : condition.getSelectAttributes()) {
      generator.writeString(attribute.getName());
    }
    generator.writeEndArray();
    generator.writeEndObject();
  }
}
