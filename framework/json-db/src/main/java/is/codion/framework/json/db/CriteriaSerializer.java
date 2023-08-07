/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.condition.AttributeCriteria;
import is.codion.framework.db.condition.Criteria;
import is.codion.framework.db.condition.Criteria.Combination;
import is.codion.framework.db.condition.CustomCriteria;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

final class CriteriaSerializer extends StdSerializer<Criteria> {

  private static final long serialVersionUID = 1;

  private final AttributeCriteriaSerializer attributeCriteriaSerializer;
  private final CriteriaCombinationSerializer criteriaCombinationSerializer;
  private final CustomCriteriaSerializer customCriteriaSerializer;
  private final Entities entities;

  CriteriaSerializer(EntityObjectMapper entityObjectMapper) {
    super(Criteria.class);
    this.attributeCriteriaSerializer = new AttributeCriteriaSerializer(entityObjectMapper);
    this.criteriaCombinationSerializer = new CriteriaCombinationSerializer(attributeCriteriaSerializer);
    this.customCriteriaSerializer = new CustomCriteriaSerializer(entityObjectMapper);
    this.entities = entityObjectMapper.entities();
  }

  @Override
  public void serialize(Criteria criteria, JsonGenerator generator,
                        SerializerProvider provider) throws IOException {
    generator.writeStartObject();
    generator.writeStringField("entityType", criteria.entityType().name());
    generator.writeFieldName("criteria");
    serialize(criteria, generator);
    generator.writeEndObject();
  }

  void serialize(Criteria criteria, JsonGenerator generator) throws IOException {
    if (criteria instanceof Combination) {
      Combination combination = (Combination) criteria;
      criteriaCombinationSerializer.serialize(combination, generator);
    }
    else if (criteria instanceof AttributeCriteria) {
      AttributeCriteria<?> attributeCriteria = (AttributeCriteria<?>) criteria;
      attributeCriteriaSerializer.serialize(attributeCriteria, generator);
    }
    else if (criteria instanceof CustomCriteria) {
      CustomCriteria customCriteria = (CustomCriteria) criteria;
      customCriteriaSerializer.serialize(customCriteria, generator);
    }
    else if (criteria.toString(entities.definition(criteria.entityType())).isEmpty()) {
      generator.writeStartObject();
      generator.writeStringField("type", "all");
      generator.writeEndObject();
    }
    else {
      throw new IllegalArgumentException("Unknown Condition type: " + criteria.getClass());
    }
  }
}
