package is.codion.framework.json.db;

import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.criteria.Criteria;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

import static is.codion.framework.db.condition.Condition.where;

public class ConditionDeserializer extends StdDeserializer<Condition> {

  private static final long serialVersionUID = 1;

  private final SelectConditionDeserializer selectConditionDeserializer;
  private final UpdateConditionDeserializer updateConditionDeserializer;
  private final CriteriaDeserializer criteriaDeserializer;
  private final Entities entities;

  ConditionDeserializer(EntityObjectMapper entityObjectMapper) {
    super(Condition.class);
    this.criteriaDeserializer = new CriteriaDeserializer(entityObjectMapper);
    this.selectConditionDeserializer = new SelectConditionDeserializer(criteriaDeserializer);
    this.updateConditionDeserializer = new UpdateConditionDeserializer(criteriaDeserializer);
    this.entities = entityObjectMapper.entities();
  }

  @Override
  public Condition deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException, JacksonException {
    JsonNode jsonNode = parser.getCodec().readTree(parser);
    String conditionType = jsonNode.get("type").asText();
    if (conditionType.equals("select")) {
      return selectConditionDeserializer.deserialize(parser, ctxt);
    }
    if (conditionType.equals("update")) {
      return updateConditionDeserializer.deserialize(parser, ctxt);
    }
    else if (conditionType.equals("default")) {
      EntityType entityType = entities.domainType().entityType(jsonNode.get("entityType").asText());
      EntityDefinition definition = entities.definition(entityType);
      JsonNode criteriaNode = jsonNode.get("criteria");
      Criteria criteria = criteriaDeserializer.deserialize(definition, criteriaNode);

      return where(criteria);
    }

    throw new IllegalArgumentException("Unknown condition type: " + conditionType);
  }
}
