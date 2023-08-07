/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.condition.UpdateCondition;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Map;

final class UpdateConditionSerializer extends StdSerializer<UpdateCondition> {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;
  private final CriteriaSerializer criteriaSerializer;

  UpdateConditionSerializer(EntityObjectMapper entityObjectMapper) {
    super(UpdateCondition.class);
    this.criteriaSerializer = new CriteriaSerializer(entityObjectMapper);
    this.entityObjectMapper = entityObjectMapper;
  }

  @Override
  public void serialize(UpdateCondition condition, JsonGenerator generator,
                        SerializerProvider provider) throws IOException {
    generator.writeStartObject();
    generator.writeStringField("type", "update");
    generator.writeStringField("entityType", condition.entityType().name());
    generator.writeFieldName("criteria");
    criteriaSerializer.serialize(condition.criteria(), generator);
    generator.writeFieldName("values");
    generator.writeStartObject();
    for (Map.Entry<Attribute<?>, Object> attributeValue : condition.attributeValues().entrySet()) {
      generator.writeFieldName(attributeValue.getKey().name());
      entityObjectMapper.writeValue(generator, attributeValue.getValue());
    }
    generator.writeEndObject();
    generator.writeEndObject();
  }
}
