/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.EntityConnection.Count;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnection.Update;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import static java.util.Objects.requireNonNull;

/**
 * ObjectMapper implementation for {@link Select} and {@link Update}.
 * For instances use the {@link #databaseObjectMapper(EntityObjectMapper)} factory method.
 */
public final class DatabaseObjectMapper extends ObjectMapper {

  private static final long serialVersionUID = 1;

  private DatabaseObjectMapper(EntityObjectMapper entityObjectMapper) {
    registerModule(requireNonNull(entityObjectMapper).module());
    SimpleModule module = new SimpleModule();
    module.addSerializer(Select.class, new SelectSerializer(entityObjectMapper));
    module.addDeserializer(Select.class, new SelectDeserializer(entityObjectMapper));
    module.addSerializer(Update.class, new UpdateSerializer(entityObjectMapper));
    module.addDeserializer(Update.class, new UpdateDeserializer(entityObjectMapper));
    module.addSerializer(Count.class, new CountSerializer(entityObjectMapper));
    module.addDeserializer(Count.class, new CountDeserializer(entityObjectMapper));
    registerModule(module);
  }

  /**
   * Instantiates a new {@link DatabaseObjectMapper}
   * @param entityObjectMapper a {@link EntityObjectMapper}
   * @return a new {@link DatabaseObjectMapper} instance
   */
  public static DatabaseObjectMapper databaseObjectMapper(EntityObjectMapper entityObjectMapper) {
    return new DatabaseObjectMapper(entityObjectMapper);
  }
}
