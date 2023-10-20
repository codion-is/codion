/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.OrderBy.NullOrder;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.Condition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class SelectDeserializer extends StdDeserializer<Select> {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;

  SelectDeserializer(EntityObjectMapper entityObjectMapper) {
    super(Select.class);
    this.entityObjectMapper = entityObjectMapper;
  }

  @Override
  public Select deserialize(JsonParser parser, DeserializationContext ctxt)
          throws IOException {
    JsonNode jsonNode = parser.getCodec().readTree(parser);
    EntityType entityType = entityObjectMapper.entities().domainType().entityType(jsonNode.get("entityType").asText());
    EntityDefinition definition = entityObjectMapper.entities().definition(entityType);
    JsonNode whereConditionNode = jsonNode.get("where");
    Condition whereCondition = entityObjectMapper.deserializeCondition(definition, whereConditionNode);

    Select.Builder selectBuilder = Select.where(whereCondition);
    JsonNode orderBy = jsonNode.get("orderBy");
    if (orderBy != null && !orderBy.isNull()) {
      selectBuilder.orderBy(deserializeOrderBy(definition, orderBy));
    }
    JsonNode limit = jsonNode.get("limit");
    if (limit != null && !limit.isNull()) {
      selectBuilder.limit(limit.asInt());
    }
    JsonNode offset = jsonNode.get("offset");
    if (offset != null && !offset.isNull()) {
      selectBuilder.offset(offset.asInt());
    }
    JsonNode forUpdate = jsonNode.get("forUpdate");
    if (forUpdate != null && !forUpdate.isNull() && forUpdate.asBoolean()) {
      selectBuilder.forUpdate();
    }
    JsonNode fetchDepth = jsonNode.get("fetchDepth");
    if (fetchDepth != null && !fetchDepth.isNull()) {
      selectBuilder.fetchDepth(fetchDepth.asInt());
    }
    JsonNode fkFetchDepth = jsonNode.get("fkFetchDepth");
    if (fkFetchDepth != null && !fkFetchDepth.isNull()) {
      for (ForeignKey foreignKey : definition.foreignKeys().get()) {
        JsonNode fetchDepthNode = fkFetchDepth.get(foreignKey.name());
        if (fetchDepthNode != null) {
          selectBuilder.fetchDepth(foreignKey, fetchDepthNode.asInt());
        }
      }
    }
    JsonNode attributes = jsonNode.get("attributes");
    if (attributes != null && !attributes.isNull()) {
      selectBuilder.attributes(deserializeAttributes(definition, attributes));
    }
    JsonNode queryTimeout = jsonNode.get("queryTimeout");
    if (queryTimeout != null && !queryTimeout.isNull()) {
      selectBuilder.queryTimeout(queryTimeout.asInt());
    }

    return selectBuilder.build();
  }

  private static OrderBy deserializeOrderBy(EntityDefinition definition, JsonNode jsonNode) {
    if (jsonNode.isEmpty()) {
      return null;
    }

    OrderBy.Builder builder = OrderBy.builder();
    for (JsonNode node : jsonNode) {
      String[] split = node.asText().split(":");
      Column<Object> column = (Column<Object>) definition.attributes().get(split[0]);
      String order = split[1];
      NullOrder nullOrder = NullOrder.valueOf(split[2]);
      if ("asc".equals(order)) {
        switch (nullOrder) {
          case NULLS_FIRST:
            builder.ascendingNullsFirst(column);
            break;
          case NULLS_LAST:
            builder.ascendingNullsLast(column);
            break;
          default:
            builder.ascending(column);
            break;
        }
      }
      else {
        switch (nullOrder) {
          case NULLS_FIRST:
            builder.descendingNullsFirst(column);
            break;
          case NULLS_LAST:
            builder.descendingNullsLast(column);
            break;
          default:
            builder.descending(column);
            break;
        }
      }
    }

    return builder.build();
  }

  private static List<Attribute<?>> deserializeAttributes(EntityDefinition definition, JsonNode jsonNode) {
    List<Attribute<?>> attributes = new ArrayList<>(jsonNode.size());
    for (JsonNode node : jsonNode) {
      attributes.add(definition.attributes().get(node.asText()));
    }

    return attributes;
  }
}
