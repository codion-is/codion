/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.criteria.AllCriteria;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.io.Serializable;

final class AllCriteriaSerializer implements Serializable {

  private static final long serialVersionUID = 1;

  void serialize(AllCriteria criteria, JsonGenerator generator) throws IOException {
    generator.writeStartObject();
    generator.writeStringField("type", "all");
    generator.writeStringField("entityType", criteria.entityType().name());
    generator.writeEndObject();
  }
}
