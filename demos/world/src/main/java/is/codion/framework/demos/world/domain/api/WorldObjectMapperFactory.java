package is.codion.framework.demos.world.domain.api;

import is.codion.framework.demos.world.domain.api.World.Location;
import is.codion.framework.domain.entity.Entities;
import is.codion.plugin.jackson.json.domain.DefaultEntityObjectMapperFactory;
import is.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

// tag::customSerializer[]
public final class WorldObjectMapperFactory extends DefaultEntityObjectMapperFactory {

  public WorldObjectMapperFactory() {
    super(World.DOMAIN);
  }

  @Override
  public EntityObjectMapper entityObjectMapper(Entities entities) {
    StdSerializer<Location> locationSerializer = new StdSerializer<Location>(Location.class) {
      @Override
      public void serialize(Location value, JsonGenerator generator,
                            SerializerProvider provider) throws IOException {
        generator.writeStartObject();
        generator.writeNumberField("lat", value.latitude());
        generator.writeNumberField("lon", value.longitude());
        generator.writeEndObject();
      }
    };

    StdDeserializer<Location> locationDeserializer = new StdDeserializer<Location>(Location.class) {
      @Override
      public Location deserialize(JsonParser parser, DeserializationContext ctxt)
              throws IOException, JsonProcessingException {
        JsonNode node = parser.getCodec().readTree(parser);

        return new Location(node.get("lat").asDouble(), node.get("lon").asDouble());
      }
    };

    EntityObjectMapper objectMapper = EntityObjectMapper.entityObjectMapper(entities);
    objectMapper.addSerializer(Location.class, locationSerializer);
    objectMapper.addDeserializer(Location.class, locationDeserializer);

    return objectMapper;
  }
}
// end::customSerializer[]
