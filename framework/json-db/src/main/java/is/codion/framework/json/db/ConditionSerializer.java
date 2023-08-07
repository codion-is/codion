package is.codion.framework.json.db;

import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.db.condition.UpdateCondition;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class ConditionSerializer extends StdSerializer<Condition> {

  private final CriteriaSerializer criteriaSerializer;
  private final SelectConditionSerializer selectConditionSerializer;
  private final UpdateConditionSerializer updateConditionSerializer;

  ConditionSerializer(EntityObjectMapper entityObjectMapper) {
    super(Condition.class);
    this.criteriaSerializer = new CriteriaSerializer(entityObjectMapper);
    this.selectConditionSerializer = new SelectConditionSerializer(entityObjectMapper);
    this.updateConditionSerializer = new UpdateConditionSerializer(entityObjectMapper);
  }

  @Override
  public void serialize(Condition condition, JsonGenerator generator, SerializerProvider provider) throws IOException {
    if (condition instanceof SelectCondition) {
      selectConditionSerializer.serialize((SelectCondition) condition, generator, provider);
    }
    else if (condition instanceof UpdateCondition) {
      updateConditionSerializer.serialize((UpdateCondition) condition, generator, provider);
    }
    else {
      generator.writeStartObject();
      generator.writeStringField("type", "default");
      generator.writeStringField("entityType", condition.entityType().name());
      generator.writeFieldName("criteria");
      criteriaSerializer.serialize(condition.criteria(), generator);
      generator.writeEndObject();
    }
  }
}
