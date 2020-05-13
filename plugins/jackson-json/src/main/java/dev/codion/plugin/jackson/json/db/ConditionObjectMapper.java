/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.plugin.jackson.json.db;

import dev.codion.framework.db.condition.EntityCondition;
import dev.codion.framework.db.condition.EntitySelectCondition;
import dev.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * ObjectMapper implementation for {@link EntityCondition} and it's subclasses
 */
public final class ConditionObjectMapper extends ObjectMapper {

  private static final long serialVersionUID = 1;

  /**
   * Instantiates a new ConditionObjectMapper
   * @param entityObjectMapper a EntityObjectMapper
   */
  public ConditionObjectMapper(final EntityObjectMapper entityObjectMapper) {
    final SimpleModule module = new SimpleModule();
    module.addSerializer(EntityCondition.class, new EntityConditionSerializer(entityObjectMapper));
    module.addDeserializer(EntityCondition.class, new EntityConditionDeserializer(entityObjectMapper));
    module.addSerializer(EntitySelectCondition.class, new EntitySelectConditionSerializer(entityObjectMapper));
    module.addDeserializer(EntitySelectCondition.class, new EntitySelectConditionDeserializer(entityObjectMapper));
    registerModule(module);
  }
}
