package is.codion.framework.demos.world.domain.api;

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
import org.jxmapviewer.viewer.GeoPosition;

import java.io.IOException;

// tag::customSerializer[]
public final class WorldObjectMapperFactory extends DefaultEntityObjectMapperFactory {

  public WorldObjectMapperFactory() {
    super(World.DOMAIN);
  }

  @Override
  public EntityObjectMapper createEntityObjectMapper(Entities entities) {
    StdSerializer<GeoPosition> positionSerializer = new StdSerializer<GeoPosition>(GeoPosition.class) {
      @Override
      public void serialize(GeoPosition value, JsonGenerator generator,
                            SerializerProvider provider) throws IOException {
        generator.writeStartObject();
        generator.writeNumberField("lat", value.getLatitude());
        generator.writeNumberField("lon", value.getLongitude());
        generator.writeEndObject();
      }
    };

    StdDeserializer<GeoPosition> positionDeserializer = new StdDeserializer<GeoPosition>(GeoPosition.class) {
      @Override
      public GeoPosition deserialize(JsonParser parser, DeserializationContext ctxt)
              throws IOException, JsonProcessingException {
        JsonNode node = parser.getCodec().readTree(parser);

        return new GeoPosition(node.get("lat").asDouble(), node.get("lon").asDouble());
      }
    };

    EntityObjectMapper objectMapper = EntityObjectMapper.createEntityObjectMapper(entities);
    objectMapper.addSerializer(GeoPosition.class, positionSerializer);
    objectMapper.addDeserializer(GeoPosition.class, positionDeserializer);

    return objectMapper;
  }
}
// end::customSerializer[]
