/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.criteria.AllCriteria;
import is.codion.framework.db.criteria.AttributeCriteria;
import is.codion.framework.db.criteria.Criteria;
import is.codion.framework.db.criteria.Criteria.Combination;
import is.codion.framework.db.criteria.CustomCriteria;
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
  private final AllCriteriaSerializer allCriteriaSerializer;

  CriteriaSerializer(EntityObjectMapper entityObjectMapper) {
    super(Criteria.class);
    this.attributeCriteriaSerializer = new AttributeCriteriaSerializer(entityObjectMapper);
    this.criteriaCombinationSerializer = new CriteriaCombinationSerializer(attributeCriteriaSerializer);
    this.customCriteriaSerializer = new CustomCriteriaSerializer(entityObjectMapper);
    this.allCriteriaSerializer = new AllCriteriaSerializer();
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
    else if (criteria instanceof AllCriteria) {
      AllCriteria allCriteria = (AllCriteria) criteria;
      allCriteriaSerializer.serialize(allCriteria, generator);
    }
    else {
      throw new IllegalArgumentException("Unknown criteria type: " + criteria.getClass());
    }
  }
}
