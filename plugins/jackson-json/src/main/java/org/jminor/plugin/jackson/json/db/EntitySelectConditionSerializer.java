/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json.db;

import org.jminor.framework.db.condition.EntitySelectCondition;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

final class EntitySelectConditionSerializer extends StdSerializer<EntitySelectCondition> {

  public EntitySelectConditionSerializer() {
    super(EntitySelectCondition.class);
  }

  @Override
  public void serialize(final EntitySelectCondition condition, final JsonGenerator generator,
                        final SerializerProvider provider) throws IOException {

  }
}
