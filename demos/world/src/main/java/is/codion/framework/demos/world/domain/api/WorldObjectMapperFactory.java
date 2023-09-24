/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.world.domain.api;

import is.codion.framework.demos.world.domain.api.World.Location;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.json.domain.DefaultEntityObjectMapperFactory;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

// tag::customSerializer[]
public final class WorldObjectMapperFactory extends DefaultEntityObjectMapperFactory {

  private static final String LATITUDE = "latitude";
  private static final String LONGITUDE = "longitude";

  public WorldObjectMapperFactory() {
    super(World.DOMAIN);
  }

  @Override
  public EntityObjectMapper entityObjectMapper(Entities entities) {
    EntityObjectMapper objectMapper = super.entityObjectMapper(entities);
    objectMapper.addSerializer(Location.class, new LocationSerializer());
    objectMapper.addDeserializer(Location.class, new LocationDeserializer());

    return objectMapper;
  }

  private static final class LocationSerializer extends StdSerializer<Location> {

    private LocationSerializer() {
      super(Location.class);
    }

    @Override
    public void serialize(Location location, JsonGenerator generator, SerializerProvider provider) throws IOException {
      generator.writeStartObject();
      generator.writeNumberField(LATITUDE, location.latitude());
      generator.writeNumberField(LONGITUDE, location.longitude());
      generator.writeEndObject();
    }
  }

  private static final class LocationDeserializer extends StdDeserializer<Location> {

    private LocationDeserializer() {
      super(Location.class);
    }

    @Override
    public Location deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException, JacksonException {
      JsonNode node = parser.getCodec().readTree(parser);

      return new Location(node.get(LATITUDE).asDouble(), node.get(LONGITUDE).asDouble());
    }
  }
}
// end::customSerializer[]
