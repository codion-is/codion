/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class SelectConditionDeserializer extends StdDeserializer<SelectCondition> {

  private static final long serialVersionUID = 1;

  private final ConditionDeserializer conditionDeserializer;
  private final Entities entities;

  SelectConditionDeserializer(final EntityObjectMapper entityObjectMapper) {
    super(SelectCondition.class);
    this.conditionDeserializer = new ConditionDeserializer(entityObjectMapper);
    this.entities = entityObjectMapper.getEntities();
  }

  @Override
  public SelectCondition deserialize(final JsonParser parser, final DeserializationContext ctxt)
          throws IOException {
    final JsonNode jsonNode = parser.getCodec().readTree(parser);
    final EntityType<?> entityType = entities.getDomainType().entityType(jsonNode.get("entityType").asText());
    final EntityDefinition definition = entities.getDefinition(entityType);
    final JsonNode conditionNode = jsonNode.get("condition");
    final Condition condition = conditionDeserializer.deserialize(definition, conditionNode);

    final SelectCondition selectCondition = condition.selectCondition();
    selectCondition.setOrderBy(deserializeOrderBy(definition, jsonNode.get("orderBy")));
    selectCondition.setLimit(jsonNode.get("limit").asInt());
    selectCondition.setOffset(jsonNode.get("offset").asInt());
    selectCondition.setFetchCount(jsonNode.get("fetchCount").asInt());
    final JsonNode fetchDepth = jsonNode.get("fetchDepth");
    if (!fetchDepth.isNull()) {
      selectCondition.setFetchDepth(fetchDepth.asInt());
    }
    final JsonNode fkFetchDepth = jsonNode.get("fkFetchDepth");
    for (final ForeignKeyProperty property : definition.getForeignKeyProperties()) {
      final JsonNode fetchDepthNode = fkFetchDepth.get(property.getAttribute().getName());
      if (fetchDepthNode != null) {
        selectCondition.setFetchDepth(property.getAttribute(), fetchDepthNode.asInt());
      }
    }
    selectCondition.setForUpdate(jsonNode.get("forUpdate").asBoolean());
    selectCondition.setSelectAttributes(deserializeSelectAttributes(definition, jsonNode.get("selectAttributes")));

    return selectCondition;
  }

  private static OrderBy deserializeOrderBy(final EntityDefinition definition, final JsonNode jsonNode) {
    if (jsonNode.isEmpty()) {
      return null;
    }

    OrderBy orderBy = OrderBy.orderBy();
    for (final JsonNode node : jsonNode) {
      final String[] split = node.asText().split(":");
      final String attributeName = split[0];
      if ("asc".equals(split[1])) {
        orderBy = orderBy.ascending(definition.getAttribute(attributeName));
      }
      else {
        orderBy = orderBy.descending(definition.getAttribute(attributeName));
      }
    }

    return orderBy;
  }

  private static Attribute<?>[] deserializeSelectAttributes(final EntityDefinition definition, final JsonNode jsonNode) {
    final List<Attribute<?>> attributes = new ArrayList<>(jsonNode.size());
    for (final JsonNode node : jsonNode) {
      attributes.add(definition.getAttribute(node.asText()));
    }

    return attributes.toArray(new Attribute[0]);
  }
}
