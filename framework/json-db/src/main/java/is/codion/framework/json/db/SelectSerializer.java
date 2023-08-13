/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

final class SelectSerializer extends StdSerializer<Select> {

  private static final long serialVersionUID = 1;

  private final ConditionSerializer conditionSerializer;

  SelectSerializer(EntityObjectMapper entityObjectMapper) {
    super(Select.class);
    this.conditionSerializer = new ConditionSerializer(entityObjectMapper);
  }

  @Override
  public void serialize(Select select, JsonGenerator generator,
                        SerializerProvider provider) throws IOException {
    generator.writeStartObject();
    generator.writeStringField("entityType", select.condition().entityType().name());
    generator.writeFieldName("condition");
    conditionSerializer.serialize(select.condition(), generator);
    OrderBy orderBy = select.orderBy().orElse(null);
    if (orderBy != null) {
      generator.writeFieldName("orderBy");
      generator.writeStartArray();
      for (OrderBy.OrderByColumn orderByColumn : orderBy.orderByColumns()) {
        generator.writeString(orderByColumn.column().name() +
                ":" + (orderByColumn.isAscending() ? "asc" : "desc") +
                ":" + orderByColumn.nullOrder().name());
      }
      generator.writeEndArray();
    }
    if (select.limit() != 0) {
      generator.writeObjectField("limit", select.limit());
    }
    if (select.offset() != 0) {
      generator.writeObjectField("offset", select.offset());
    }
    if (select.forUpdate()) {
      generator.writeObjectField("forUpdate", select.forUpdate());
    }
    if (select.queryTimeout() != 0) {
      generator.writeObjectField("queryTimeout", select.queryTimeout());
    }
    Integer conditionFetchDepth = select.fetchDepth().orElse(null);
    if (conditionFetchDepth != null) {
      generator.writeObjectField("fetchDepth", conditionFetchDepth);
    }
    Map<ForeignKey, Integer> foreignKeyFetchDepths = select.foreignKeyFetchDepths();
    if (!foreignKeyFetchDepths.isEmpty()) {
      generator.writeFieldName("fkFetchDepth");
      generator.writeStartObject();
      for (Map.Entry<ForeignKey, Integer> entry : foreignKeyFetchDepths.entrySet()) {
        generator.writeObjectField(entry.getKey().name(), entry.getValue());
      }
      generator.writeEndObject();
    }
    Collection<Attribute<?>> attributes = select.attributes();
    if (!attributes.isEmpty()) {
      generator.writeFieldName("attributes");
      generator.writeStartArray();
      for (Attribute<?> attribute : attributes) {
        generator.writeString(attribute.name());
      }
      generator.writeEndArray();
      generator.writeEndObject();
    }
  }
}
