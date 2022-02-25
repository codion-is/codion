/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.domain;

import is.codion.framework.domain.entity.Entities;
import is.codion.plugin.jackson.json.TestDomain;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public final class CustomEntityObjectMapperFactory extends DefaultEntityObjectMapperFactory {

  public CustomEntityObjectMapperFactory() {
    super(TestDomain.DOMAIN);
  }

  @Override
  public EntityObjectMapper createEntityObjectMapper(Entities entities) {
    EntityObjectMapper mapper = EntityObjectMapper.createEntityObjectMapper(entities);
    mapper.addSerializer(Custom.class, new StdSerializer<Custom>(Custom.class) {
      @Override
      public void serialize(Custom value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("value", value.value);
        gen.writeEndObject();
      }
    });
    mapper.addDeserializer(Custom.class, new StdDeserializer<Custom>(Custom.class) {
      @Override
      public Custom deserialize(JsonParser p, DeserializationContext ctxt) throws IOException,
              JsonProcessingException {
        JsonNode node = p.getCodec().readTree(p);

        return new Custom(node.get("value").asText());
      }
    });

    return mapper;
  }
}
