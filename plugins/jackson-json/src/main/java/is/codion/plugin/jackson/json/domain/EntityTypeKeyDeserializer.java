package is.codion.plugin.jackson.json.domain;

import is.codion.framework.domain.entity.Entities;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

import java.io.IOException;

class EntityTypeKeyDeserializer extends KeyDeserializer {

  private final Entities entities;

  EntityTypeKeyDeserializer(final Entities entities) {
    this.entities = entities;
  }

  @Override
  public Object deserializeKey(final String key, final DeserializationContext ctxt) throws IOException {
    return entities.getDomainType().entityType(key);
  }
}
