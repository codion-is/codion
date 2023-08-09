/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.db.criteria.Criteria;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.OrderBy.NullOrder;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static is.codion.framework.db.condition.SelectCondition.builder;

final class SelectConditionDeserializer extends StdDeserializer<SelectCondition> {

  private static final long serialVersionUID = 1;

  private final CriteriaDeserializer criteriaDeserializer;
  private final Entities entities;

  SelectConditionDeserializer(CriteriaDeserializer criteriaDeserializer) {
    super(SelectCondition.class);
    this.criteriaDeserializer = criteriaDeserializer;
    this.entities = criteriaDeserializer.entities;
  }

  @Override
  public SelectCondition deserialize(JsonParser parser, DeserializationContext ctxt)
          throws IOException {
    JsonNode jsonNode = parser.getCodec().readTree(parser);
    EntityType entityType = entities.domainType().entityType(jsonNode.get("entityType").asText());
    EntityDefinition definition = entities.definition(entityType);
    JsonNode criteriaNode = jsonNode.get("criteria");
    Criteria criteria = criteriaDeserializer.deserialize(definition, criteriaNode);

    SelectCondition.Builder selectCondition = builder(criteria);
    JsonNode orderBy = jsonNode.get("orderBy");
    if (orderBy != null && !orderBy.isNull()) {
      selectCondition.orderBy(deserializeOrderBy(definition, orderBy));
    }
    JsonNode limit = jsonNode.get("limit");
    if (limit != null && !limit.isNull()) {
      selectCondition.limit(limit.asInt());
    }
    JsonNode offset = jsonNode.get("offset");
    if (offset != null && !offset.isNull()) {
      selectCondition.offset(offset.asInt());
    }
    JsonNode fetchDepth = jsonNode.get("fetchDepth");
    if (fetchDepth != null && !fetchDepth.isNull()) {
      selectCondition.fetchDepth(fetchDepth.asInt());
    }
    JsonNode fkFetchDepth = jsonNode.get("fkFetchDepth");
    if (fkFetchDepth != null && !fkFetchDepth.isNull()) {
      for (ForeignKey foreignKey : definition.foreignKeys()) {
        JsonNode fetchDepthNode = fkFetchDepth.get(foreignKey.name());
        if (fetchDepthNode != null) {
          selectCondition.fetchDepth(foreignKey, fetchDepthNode.asInt());
        }
      }
    }
    JsonNode forUpdate = jsonNode.get("forUpdate");
    if (forUpdate != null && !forUpdate.isNull() && forUpdate.asBoolean()) {
      selectCondition.forUpdate();
    }
    JsonNode selectAttributes = jsonNode.get("selectAttributes");
    if (selectAttributes != null && !selectAttributes.isNull()) {
      selectCondition.selectAttributes(deserializeSelectAttributes(definition, selectAttributes));
    }
    JsonNode queryTimeout = jsonNode.get("queryTimeout");
    if (queryTimeout != null && !queryTimeout.isNull()) {
      selectCondition.queryTimeout(queryTimeout.asInt());
    }

    return selectCondition.build();
  }

  private static OrderBy deserializeOrderBy(EntityDefinition definition, JsonNode jsonNode) {
    if (jsonNode.isEmpty()) {
      return null;
    }

    OrderBy.Builder builder = OrderBy.builder();
    for (JsonNode node : jsonNode) {
      String[] split = node.asText().split(":");
      Attribute<Object> attribute = definition.attribute(split[0]);
      String order = split[1];
      NullOrder nullOrder = NullOrder.valueOf(split[2]);
      if ("asc".equals(order)) {
        switch (nullOrder) {
          case NULLS_FIRST:
            builder.ascendingNullsFirst(attribute);
            break;
          case NULLS_LAST:
            builder.ascendingNullsLast(attribute);
            break;
          default:
            builder.ascending(attribute);
            break;
        }
      }
      else {
        switch (nullOrder) {
          case NULLS_FIRST:
            builder.descendingNullsFirst(attribute);
            break;
          case NULLS_LAST:
            builder.descendingNullsLast(attribute);
            break;
          default:
            builder.descending(attribute);
            break;
        }
      }
    }

    return builder.build();
  }

  private static Attribute<?>[] deserializeSelectAttributes(EntityDefinition definition, JsonNode jsonNode) {
    List<Attribute<?>> attributes = new ArrayList<>(jsonNode.size());
    for (JsonNode node : jsonNode) {
      attributes.add(definition.attribute(node.asText()));
    }

    return attributes.toArray(new Attribute[0]);
  }
}
