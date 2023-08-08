/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.db.condition.UpdateCondition;
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
    module.addSerializer(Condition.class, new ConditionSerializer(entityObjectMapper));
    module.addDeserializer(Condition.class, new ConditionDeserializer(entityObjectMapper));
    module.addSerializer(SelectCondition.class, new SelectConditionSerializer(entityObjectMapper));
    module.addDeserializer(SelectCondition.class, new SelectConditionDeserializer(criteriaDeserializer));
    module.addSerializer(UpdateCondition.class, new UpdateConditionSerializer(entityObjectMapper));
    module.addDeserializer(UpdateCondition.class, new UpdateConditionDeserializer(criteriaDeserializer));
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
