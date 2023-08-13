/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.condition.Select;
import is.codion.framework.db.condition.Update;
import is.codion.framework.db.criteria.Criteria;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import static java.util.Objects.requireNonNull;

/**
 * ObjectMapper implementation for {@link Criteria} and it's subclasses.
 * For instances use the {@link #conditionObjectMapper(EntityObjectMapper)} factory method.
 */
public final class ConditionObjectMapper extends ObjectMapper {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;

  private ConditionObjectMapper(EntityObjectMapper entityObjectMapper) {
    this.entityObjectMapper = requireNonNull(entityObjectMapper);
    SimpleModule module = new SimpleModule();
    CriteriaDeserializer criteriaDeserializer = new CriteriaDeserializer(entityObjectMapper);
    module.addSerializer(Criteria.class, new CriteriaSerializer(entityObjectMapper));
    module.addDeserializer(Criteria.class, criteriaDeserializer);
    module.addSerializer(Select.class, new SelectSerializer(entityObjectMapper));
    module.addDeserializer(Select.class, new SelectDeserializer(criteriaDeserializer));
    module.addSerializer(Update.class, new UpdateSerializer(entityObjectMapper));
    module.addDeserializer(Update.class, new UpdateDeserializer(criteriaDeserializer));
    registerModule(module);
  }

  /**
   * @return the {@link EntityObjectMapper} this {@link ConditionObjectMapper} uses.
   */
  public EntityObjectMapper entityObjectMapper() {
    return entityObjectMapper;
  }

  /**
   * Instantiates a new {@link ConditionObjectMapper}
   * @param entityObjectMapper a {@link EntityObjectMapper}
   * @return a new {@link ConditionObjectMapper} instance
   */
  public static ConditionObjectMapper conditionObjectMapper(EntityObjectMapper entityObjectMapper) {
    return new ConditionObjectMapper(entityObjectMapper);
  }
}
