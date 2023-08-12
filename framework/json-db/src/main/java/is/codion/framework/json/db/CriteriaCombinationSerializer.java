/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.criteria.ColumnCriteria;
import is.codion.framework.db.criteria.Criteria;
import is.codion.framework.db.criteria.Criteria.Combination;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.io.Serializable;

import static java.util.Objects.requireNonNull;

final class CriteriaCombinationSerializer implements Serializable {

  private static final long serialVersionUID = 1;

  private final ColumnCriteriaSerializer columnCriteriaSerializer;

  CriteriaCombinationSerializer(ColumnCriteriaSerializer columnCriteriaSerializer) {
    this.columnCriteriaSerializer = requireNonNull(columnCriteriaSerializer);
  }

  void serialize(Combination combination, JsonGenerator generator) throws IOException {
    generator.writeStartObject();
    generator.writeStringField("type", "combination");
    generator.writeStringField("conjunction", combination.conjunction().name());
    generator.writeArrayFieldStart("criteria");
    for (Criteria criteria : combination.criteria()) {
      if (criteria instanceof Combination) {
        serialize((Combination) criteria, generator);
      }
      else if (criteria instanceof ColumnCriteria) {
        columnCriteriaSerializer.serialize((ColumnCriteria<?>) criteria, generator);
      }
    }
    generator.writeEndArray();
    generator.writeEndObject();
  }
}
