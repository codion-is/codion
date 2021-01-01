/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.UpdateCondition;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.plugin.jackson.json.domain.EntityDeserializer;
import is.codion.plugin.jackson.json.domain.EntityObjectMapper;

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

  UpdateConditionDeserializer(final EntityObjectMapper entityObjectMapper) {
    super(UpdateCondition.class);
    this.conditionDeserializer = new ConditionDeserializer(entityObjectMapper);
    this.entityObjectMapper = entityObjectMapper;
    this.entities = entityObjectMapper.getEntities();
  }

  @Override
  public UpdateCondition deserialize(final JsonParser parser, final DeserializationContext ctxt)
          throws IOException {
    final JsonNode jsonNode = parser.getCodec().readTree(parser);
    final EntityType<?> entityType = entities.getDomainType().entityType(jsonNode.get("entityType").asText());
    final EntityDefinition definition = entities.getDefinition(entityType);
    final JsonNode conditionNode = jsonNode.get("condition");
    final Condition condition = conditionDeserializer.deserialize(definition, conditionNode);

    final UpdateCondition updateCondition = condition.update();
    final JsonNode values = jsonNode.get("values");
    final Iterator<Map.Entry<String, JsonNode>> fields = values.fields();
    while (fields.hasNext()) {
      final Map.Entry<String, JsonNode> field = fields.next();
      final Attribute<Object> attribute = definition.getProperty(definition.getAttribute(field.getKey())).getAttribute();
      updateCondition.set(attribute, EntityDeserializer.parseValue(entityObjectMapper, attribute, field.getValue()));
    }

    return updateCondition;
  }
}
