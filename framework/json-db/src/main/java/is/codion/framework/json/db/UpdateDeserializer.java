/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.EntityConnection.Update;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.condition.Condition;
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

  UpdateDeserializer(EntityObjectMapper entityObjectMapper) {
    super(Update.class);
    this.entityObjectMapper = entityObjectMapper;
  }

  @Override
  public Update deserialize(JsonParser parser, DeserializationContext ctxt)
          throws IOException {
    JsonNode jsonNode = parser.getCodec().readTree(parser);
    EntityType entityType = entityObjectMapper.entities().domainType().entityType(jsonNode.get("entityType").asText());
    EntityDefinition definition = entityObjectMapper.entities().definition(entityType);
    JsonNode conditionNode = jsonNode.get("condition");
    Condition condition = entityObjectMapper.deserializeCondition(definition, conditionNode);

    Update.Builder updateBuilder = Update.where(condition);
    JsonNode values = jsonNode.get("values");
    Iterator<Map.Entry<String, JsonNode>> fields = values.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      Column<Object> column = definition.columns().definition((Column<Object>) definition.attributes().get(field.getKey())).attribute();
      updateBuilder.set(column, entityObjectMapper.readValue(field.getValue().toString(), column.type().valueClass()));
    }

    return updateBuilder.build();
  }
}
