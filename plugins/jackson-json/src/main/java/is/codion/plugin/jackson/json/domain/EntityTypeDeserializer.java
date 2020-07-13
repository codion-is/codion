package is.codion.plugin.jackson.json.domain;

import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityType;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

final class EntityTypeDeserializer extends StdDeserializer<EntityType> {

  private final Entities entities;

  EntityTypeDeserializer(final Entities entities) {
    super(EntityType.class);
    this.entities = entities;
  }

  @Override
  public EntityType deserialize(final JsonParser parser, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
    final JsonNode entityTypeNode = parser.getCodec().readTree(parser);

    return entities.getDomainType().entityType(entityTypeNode.get("name").asText());
  }
}
