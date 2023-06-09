/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.UpdateCondition;
import is.codion.framework.domain.entity.Attribute;
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

final class UpdateConditionDeserializer extends StdDeserializer<UpdateCondition> {

  private static final long serialVersionUID = 1;

  private final ConditionDeserializer conditionDeserializer;
  private final EntityObjectMapper entityObjectMapper;
  private final Entities entities;

  UpdateConditionDeserializer(EntityObjectMapper entityObjectMapper) {
    super(UpdateCondition.class);
    this.conditionDeserializer = new ConditionDeserializer(entityObjectMapper);
    this.entityObjectMapper = entityObjectMapper;
    this.entities = entityObjectMapper.entities();
  }

  @Override
  public UpdateCondition deserialize(JsonParser parser, DeserializationContext ctxt)
          throws IOException {
    JsonNode jsonNode = parser.getCodec().readTree(parser);
    EntityType entityType = entities.domainType().entityType(jsonNode.get("entityType").asText());
    EntityDefinition definition = entities.definition(entityType);
    JsonNode conditionNode = jsonNode.get("condition");
    Condition condition = conditionDeserializer.deserialize(definition, conditionNode);

    UpdateCondition.Builder updateCondition = condition.updateBuilder();
    JsonNode values = jsonNode.get("values");
    Iterator<Map.Entry<String, JsonNode>> fields = values.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      Attribute<Object> attribute = definition.property(definition.attribute(field.getKey())).attribute();
      updateCondition.set(attribute, entityObjectMapper.readValue(field.getValue().toString(), attribute.valueClass()));
    }

    return updateCondition.build();
  }
}