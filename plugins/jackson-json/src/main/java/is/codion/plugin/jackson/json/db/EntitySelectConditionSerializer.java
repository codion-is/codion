/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.framework.db.condition.SelectCondition;
import is.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

final class EntitySelectConditionSerializer extends StdSerializer<SelectCondition> {

  private static final long serialVersionUID = 1;

  private final ConditionSerializer conditionSerializer;

  EntitySelectConditionSerializer(final EntityObjectMapper entityObjectMapper) {
    super(SelectCondition.class);
    this.conditionSerializer = new ConditionSerializer(new AttributeConditionSerializer(entityObjectMapper), entityObjectMapper);
  }

  @Override
  public void serialize(final SelectCondition condition, final JsonGenerator generator,
                        final SerializerProvider provider) throws IOException {
    generator.writeStartObject();
    generator.writeStringField("entityType", condition.getEntityType().getName());
    generator.writeFieldName("condition");
    conditionSerializer.serialize(condition, generator);
    generator.writeFieldName("orderBy");
    generator.writeObject(condition.getOrderBy());
    generator.writeFieldName("limit");
    generator.writeObject(condition.getLimit());
    generator.writeFieldName("offset");
    generator.writeObject(condition.getOffset());
    generator.writeFieldName("forUpdate");
    generator.writeObject(condition.isForUpdate());
    generator.writeFieldName("fetchCount");
    generator.writeObject(condition.getFetchCount());
    generator.writeEndObject();
  }
}
