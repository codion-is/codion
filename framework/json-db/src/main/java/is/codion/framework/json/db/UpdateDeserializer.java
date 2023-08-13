/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.Update;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.domain.entity.Column;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

final class UpdateDeserializer extends StdDeserializer<Update> {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;
  private final ConditionDeserializer conditionDeserializer;
  private final Entities entities;

  UpdateDeserializer(ConditionDeserializer conditionDeserializer) {
    super(Update.class);
    this.conditionDeserializer = conditionDeserializer;
    this.entityObjectMapper = conditionDeserializer.entityObjectMapper;
    this.entities = conditionDeserializer.entities;
  }

  @Override
  public Update deserialize(JsonParser parser, DeserializationContext ctxt)
          throws IOException {
    JsonNode jsonNode = parser.getCodec().readTree(parser);
    EntityType entityType = entities.domainType().entityType(jsonNode.get("entityType").asText());
    EntityDefinition definition = entities.definition(entityType);
    JsonNode conditionNode = jsonNode.get("condition");
    Condition condition = conditionDeserializer.deserialize(definition, conditionNode);

    Update.Builder updateCondition = Update.where(condition);
    JsonNode values = jsonNode.get("values");
    Iterator<Map.Entry<String, JsonNode>> fields = values.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      Column<Object> column = definition.columnProperty((Column<Object>) definition.attribute(field.getKey())).attribute();
      updateCondition.set(column, entityObjectMapper.readValue(field.getValue().toString(), column.valueClass()));
    }

    return updateCondition.build();
  }
}
