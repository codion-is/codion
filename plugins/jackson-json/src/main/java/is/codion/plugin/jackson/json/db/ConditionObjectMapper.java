/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.db.condition.UpdateCondition;
import is.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * ObjectMapper implementation for {@link Condition} and it's subclasses
 */
public final class ConditionObjectMapper extends ObjectMapper {

  private static final long serialVersionUID = 1;

  private final EntityObjectMapper entityObjectMapper;

  /**
   * Instantiates a new ConditionObjectMapper
   * @param entityObjectMapper a EntityObjectMapper
   */
  public ConditionObjectMapper(final EntityObjectMapper entityObjectMapper) {
    this.entityObjectMapper = entityObjectMapper;
    SimpleModule module = new SimpleModule();
    module.addSerializer(Condition.class, new ConditionSerializer(entityObjectMapper));
    module.addDeserializer(Condition.class, new ConditionDeserializer(entityObjectMapper));
    module.addSerializer(SelectCondition.class, new SelectConditionSerializer(entityObjectMapper));
    module.addDeserializer(SelectCondition.class, new SelectConditionDeserializer(entityObjectMapper));
    module.addSerializer(UpdateCondition.class, new UpdateConditionSerializer(entityObjectMapper));
    module.addDeserializer(UpdateCondition.class, new UpdateConditionDeserializer(entityObjectMapper));
    registerModule(module);
  }

  public EntityObjectMapper getEntityObjectMapper() {
    return entityObjectMapper;
  }
}
