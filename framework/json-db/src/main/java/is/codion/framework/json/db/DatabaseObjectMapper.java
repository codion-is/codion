/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.EntityConnection.Count;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnection.Update;
import is.codion.framework.domain.entity.condition.Condition;
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

  private final EntityObjectMapper entityObjectMapper;

  private DatabaseObjectMapper(EntityObjectMapper entityObjectMapper) {
    this.entityObjectMapper = requireNonNull(entityObjectMapper);
    SimpleModule module = new SimpleModule();
    module.addSerializer(Condition.class, entityObjectMapper.conditionSerializer());
    module.addDeserializer(Condition.class, entityObjectMapper.conditionDeserializer());
    module.addSerializer(Select.class, new SelectSerializer(entityObjectMapper));
    module.addDeserializer(Select.class, new SelectDeserializer(entityObjectMapper));
    module.addSerializer(Update.class, new UpdateSerializer(entityObjectMapper));
    module.addDeserializer(Update.class, new UpdateDeserializer(entityObjectMapper));
    module.addSerializer(Count.class, new CountSerializer(entityObjectMapper));
    module.addDeserializer(Count.class, new CountDeserializer(entityObjectMapper));
    registerModule(module);
  }

  /**
   * @return the {@link EntityObjectMapper} this {@link DatabaseObjectMapper} uses.
   */
  public EntityObjectMapper entityObjectMapper() {
    return entityObjectMapper;
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
