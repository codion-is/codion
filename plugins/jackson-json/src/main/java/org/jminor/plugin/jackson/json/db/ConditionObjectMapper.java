/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json.db;

import org.jminor.framework.db.condition.PropertyCondition;
import org.jminor.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public final class ConditionObjectMapper extends ObjectMapper {

  public ConditionObjectMapper(final EntityObjectMapper entityObjectMapper) {
    final SimpleModule module = new SimpleModule();
    module.addSerializer(PropertyCondition.class, new PropertyConditionSerializer(entityObjectMapper));
    module.addDeserializer(PropertyCondition.class, new PropertyConditionDeserializer(entityObjectMapper));
    registerModule(module);
  }
}
