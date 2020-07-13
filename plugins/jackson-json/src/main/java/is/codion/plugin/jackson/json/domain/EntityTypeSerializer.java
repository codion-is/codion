package is.codion.plugin.jackson.json.domain;

import is.codion.framework.domain.entity.EntityType;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

import static java.util.Objects.requireNonNull;
//todo remove
public final class EntityTypeSerializer extends StdSerializer<EntityType> {

  EntityTypeSerializer() {
    super(EntityType.class);
  }

  @Override
  public void serialize(final EntityType value, final JsonGenerator generator, final SerializerProvider provider) throws IOException {
    requireNonNull(value, "value");
    generator.writeFieldName("name");
    generator.writeString(value.getName());
  }
}
