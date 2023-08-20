/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.domain;

import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityDefinition;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class EntityTypeKeyDeserializer extends KeyDeserializer {

  private final Map<String, EntityDefinition> definitions = new ConcurrentHashMap<>();
  private final Entities entities;

  EntityTypeKeyDeserializer(Entities entities) {
    this.entities = entities;
  }

  @Override
  public Object deserializeKey(String key, DeserializationContext ctxt) {
    return definitions.computeIfAbsent(key, entities::definition).entityType();
  }
}
