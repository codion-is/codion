/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json.db;

import org.jminor.framework.db.condition.EntityCondition;
import org.jminor.framework.db.condition.EntitySelectCondition;
import org.jminor.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public final class ConditionObjectMapper extends ObjectMapper {

  public ConditionObjectMapper(final EntityObjectMapper entityObjectMapper) {
    final SimpleModule module = new SimpleModule();
    module.addSerializer(EntityCondition.class, new EntityConditionSerializer(entityObjectMapper));
    module.addDeserializer(EntityCondition.class, new EntityConditionDeserializer(entityObjectMapper));
    module.addSerializer(EntitySelectCondition.class, new EntitySelectConditionSerializer());
    module.addDeserializer(EntitySelectCondition.class, new EntitySelectConditionDeserializer());
    registerModule(module);
  }
}
