/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.SelectCondition;
import is.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * ObjectMapper implementation for {@link Condition} and it's subclasses
 */
public final class ConditionObjectMapper extends ObjectMapper {

  private static final long serialVersionUID = 1;

  /**
   * Instantiates a new ConditionObjectMapper
   * @param entityObjectMapper a EntityObjectMapper
   */
  public ConditionObjectMapper(final EntityObjectMapper entityObjectMapper) {
    final SimpleModule module = new SimpleModule();
    module.addSerializer(Condition.class, new EntityConditionSerializer(entityObjectMapper));
    module.addDeserializer(Condition.class, new EntityConditionDeserializer(entityObjectMapper));
    module.addSerializer(SelectCondition.class, new EntitySelectConditionSerializer(entityObjectMapper));
    module.addDeserializer(SelectCondition.class, new EntitySelectConditionDeserializer(entityObjectMapper));
    registerModule(module);
  }
}
