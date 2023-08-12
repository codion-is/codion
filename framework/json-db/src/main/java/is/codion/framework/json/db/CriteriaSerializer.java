/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.criteria.AllCriteria;
import is.codion.framework.db.criteria.ColumnCriteria;
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

  private final ColumnCriteriaSerializer columnCriteriaSerializer;
  private final CriteriaCombinationSerializer criteriaCombinationSerializer;
  private final CustomCriteriaSerializer customCriteriaSerializer;

  CriteriaSerializer(EntityObjectMapper entityObjectMapper) {
    super(Criteria.class);
    this.columnCriteriaSerializer = new ColumnCriteriaSerializer(entityObjectMapper);
    this.criteriaCombinationSerializer = new CriteriaCombinationSerializer(columnCriteriaSerializer);
    this.customCriteriaSerializer = new CustomCriteriaSerializer(entityObjectMapper);
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
    else if (criteria instanceof ColumnCriteria) {
      ColumnCriteria<?> columnCriteria = (ColumnCriteria<?>) criteria;
      columnCriteriaSerializer.serialize(columnCriteria, generator);
    }
    else if (criteria instanceof CustomCriteria) {
      CustomCriteria customCriteria = (CustomCriteria) criteria;
      customCriteriaSerializer.serialize(customCriteria, generator);
    }
    else if (criteria instanceof AllCriteria) {
      generator.writeStartObject();
      generator.writeStringField("type", "all");
      generator.writeStringField("entityType", criteria.entityType().name());
      generator.writeEndObject();
    }
    else {
      throw new IllegalArgumentException("Unknown criteria type: " + criteria.getClass());
    }
  }
}
