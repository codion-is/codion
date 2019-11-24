/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json.db;

import org.jminor.framework.db.condition.EntitySelectCondition;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

final class EntitySelectConditionDeserializer extends StdDeserializer<EntitySelectCondition> {

  public EntitySelectConditionDeserializer() {
    super(EntitySelectCondition.class);
  }

  @Override
  public EntitySelectCondition deserialize(final JsonParser parser, final DeserializationContext ctxt)
          throws IOException, JsonProcessingException {
    return null;
  }
}
