/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.plugin.jackson.json.db;

import dev.codion.framework.db.condition.EntitySelectCondition;
import dev.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

final class EntitySelectConditionDeserializer extends StdDeserializer<EntitySelectCondition> {

  private static final long serialVersionUID = 1;

  private final ConditionSerializer conditionSerializer;

  EntitySelectConditionDeserializer(final EntityObjectMapper entityObjectMapper) {
    super(EntitySelectCondition.class);
    this.conditionSerializer = new ConditionSerializer(new PropertyConditionSerializer(entityObjectMapper), entityObjectMapper);
  }

  @Override
  public EntitySelectCondition deserialize(final JsonParser parser, final DeserializationContext ctxt)
          throws IOException {
    return null;
  }
}
