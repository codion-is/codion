/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.framework.db.condition.UpdateCondition;
import is.codion.framework.domain.entity.Attribute;
import is.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Map;

final class UpdateConditionSerializer extends StdSerializer<UpdateCondition> {

  private static final long serialVersionUID = 1;

  private final ConditionSerializer conditionSerializer;
  private final EntityObjectMapper entityObjectMapper;

  UpdateConditionSerializer(final EntityObjectMapper entityObjectMapper) {
    super(UpdateCondition.class);
    this.conditionSerializer = new ConditionSerializer(entityObjectMapper);
    this.entityObjectMapper = entityObjectMapper;
  }

  @Override
  public void serialize(final UpdateCondition condition, final JsonGenerator generator,
                        final SerializerProvider provider) throws IOException {
    generator.writeStartObject();
    generator.writeStringField("entityType", condition.getEntityType().getName());
    generator.writeFieldName("condition");
    conditionSerializer.serialize(condition.getCondition(), generator);
    generator.writeFieldName("values");
    generator.writeStartObject();
    for (final Map.Entry<Attribute<?>, Object> attributeValue : condition.getAttributeValues().entrySet()) {
      generator.writeFieldName(attributeValue.getKey().getName());
      entityObjectMapper.writeValue(generator, attributeValue.getValue());
    }
    generator.writeEndObject();
    generator.writeEndObject();
  }
}
