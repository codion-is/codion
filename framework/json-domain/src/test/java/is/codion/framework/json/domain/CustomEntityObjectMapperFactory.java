/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.domain;

import is.codion.framework.domain.entity.Entities;
import is.codion.framework.json.TestDomain;

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
  public EntityObjectMapper entityObjectMapper(Entities entities) {
    EntityObjectMapper mapper = super.entityObjectMapper(entities);
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
