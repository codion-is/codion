/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.OrderBy;
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
    JsonNode jsonNode = parser.getCodec().readTree(parser);
    EntityType entityType = entities.getDomainType().entityType(jsonNode.get("entityType").asText());
    EntityDefinition definition = entities.getDefinition(entityType);
    JsonNode conditionNode = jsonNode.get("condition");
    Condition condition = conditionDeserializer.deserialize(definition, conditionNode);

    SelectCondition selectCondition = condition.toSelectCondition();
    JsonNode orderBy = jsonNode.get("orderBy");
    if (orderBy != null && !orderBy.isNull()) {
      selectCondition = selectCondition.orderBy(deserializeOrderBy(definition, orderBy));
    }
    JsonNode limit = jsonNode.get("limit");
    if (limit != null && !limit.isNull()) {
      selectCondition = selectCondition.limit(limit.asInt());
    }
    JsonNode offset = jsonNode.get("offset");
    if (offset != null && !offset.isNull()) {
      selectCondition = selectCondition.offset(offset.asInt());
    }
    JsonNode fetchDepth = jsonNode.get("fetchDepth");
    if (fetchDepth != null && !fetchDepth.isNull()) {
      selectCondition = selectCondition.fetchDepth(fetchDepth.asInt());
    }
    JsonNode fkFetchDepth = jsonNode.get("fkFetchDepth");
    if (fkFetchDepth != null && !fkFetchDepth.isNull()) {
      for (final ForeignKey foreignKey : definition.getForeignKeys()) {
        JsonNode fetchDepthNode = fkFetchDepth.get(foreignKey.getName());
        if (fetchDepthNode != null) {
          selectCondition = selectCondition.fetchDepth(foreignKey, fetchDepthNode.asInt());
        }
      }
    }
    JsonNode forUpdate = jsonNode.get("forUpdate");
    if (forUpdate != null && !forUpdate.isNull() && forUpdate.asBoolean()) {
      selectCondition = selectCondition.forUpdate();
    }
    JsonNode selectAttributes = jsonNode.get("selectAttributes");
    if (selectAttributes != null && !selectAttributes.isNull()) {
      selectCondition = selectCondition.selectAttributes(deserializeSelectAttributes(definition, selectAttributes));
    }

    return selectCondition;
  }

  private static OrderBy deserializeOrderBy(final EntityDefinition definition, final JsonNode jsonNode) {
    if (jsonNode.isEmpty()) {
      return null;
    }

    OrderBy orderBy = OrderBy.orderBy();
    for (final JsonNode node : jsonNode) {
      String[] split = node.asText().split(":");
      String attributeName = split[0];
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
    List<Attribute<?>> attributes = new ArrayList<>(jsonNode.size());
    for (final JsonNode node : jsonNode) {
      attributes.add(definition.getAttribute(node.asText()));
    }

    return attributes.toArray(new Attribute[0]);
  }
}
