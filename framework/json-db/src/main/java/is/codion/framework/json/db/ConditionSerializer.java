/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.condition.AllCondition;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class ConditionSerializer extends StdSerializer<Condition> {

  private final CriteriaSerializer criteriaSerializer;

  ConditionSerializer(EntityObjectMapper entityObjectMapper) {
    super(Condition.class);
    this.criteriaSerializer = new CriteriaSerializer(entityObjectMapper);
  }

  @Override
  public void serialize(Condition condition, JsonGenerator generator, SerializerProvider provider) throws IOException {
    generator.writeStartObject();
    generator.writeStringField("entityType", condition.entityType().name());
    if (!(condition instanceof AllCondition)) {
      generator.writeFieldName("criteria");
      criteriaSerializer.serialize(condition.criteria(), generator);
    }
    generator.writeEndObject();
  }
}
