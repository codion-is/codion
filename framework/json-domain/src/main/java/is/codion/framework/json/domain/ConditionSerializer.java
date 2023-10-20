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
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.json.domain;

import is.codion.framework.domain.entity.attribute.ColumnCondition;
import is.codion.framework.domain.entity.attribute.Condition;
import is.codion.framework.domain.entity.attribute.Condition.All;
import is.codion.framework.domain.entity.attribute.Condition.Combination;
import is.codion.framework.domain.entity.attribute.CustomCondition;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

final class ConditionSerializer extends StdSerializer<Condition> {

  private static final long serialVersionUID = 1;

  private final ColumnConditionSerializer columnConditionSerializer;
  private final ConditionCombinationSerializer conditionCombinationSerializer;
  private final CustomConditionSerializer customConditionSerializer;

  ConditionSerializer(EntityObjectMapper entityObjectMapper) {
    super(Condition.class);
    this.columnConditionSerializer = new ColumnConditionSerializer(entityObjectMapper);
    this.conditionCombinationSerializer = new ConditionCombinationSerializer(columnConditionSerializer);
    this.customConditionSerializer = new CustomConditionSerializer(entityObjectMapper);
  }

  @Override
  public void serialize(Condition condition, JsonGenerator generator,
                        SerializerProvider provider) throws IOException {
    generator.writeStartObject();
    generator.writeStringField("entityType", condition.entityType().name());
    generator.writeFieldName("condition");
    serialize(condition, generator);
    generator.writeEndObject();
  }

  void serialize(Condition condition, JsonGenerator generator) throws IOException {
    if (condition instanceof Combination) {
      Combination combination = (Combination) condition;
      conditionCombinationSerializer.serialize(combination, generator);
    }
    else if (condition instanceof ColumnCondition) {
      ColumnCondition<?> columnCondition = (ColumnCondition<?>) condition;
      columnConditionSerializer.serialize(columnCondition, generator);
    }
    else if (condition instanceof CustomCondition) {
      CustomCondition customCondition = (CustomCondition) condition;
      customConditionSerializer.serialize(customCondition, generator);
    }
    else if (condition instanceof All) {
      generator.writeStartObject();
      generator.writeStringField("type", "all");
      generator.writeStringField("entityType", condition.entityType().name());
      generator.writeEndObject();
    }
    else {
      throw new IllegalArgumentException("Unknown condition type: " + condition.getClass());
    }
  }
}
