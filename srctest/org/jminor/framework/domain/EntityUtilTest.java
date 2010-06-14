/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.framework.Configuration;

import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class EntityUtilTest {

  @Test(expected = RuntimeException.class)
  public void getEntitySerializerUnconfigured() {
    Configuration.setValue(Configuration.ENTITY_SERIALIZER_CLASS, null);
    EntityUtil.getEntitySerializer();
  }

  @Test(expected = RuntimeException.class)
  public void getEntityDeserializerUnconfigured() {
    Configuration.setValue(Configuration.ENTITY_DESERIALIZER_CLASS, null);
    EntityUtil.getEntityDeserializer();
  }

  @Test
  public void getEntitySerializer() {
    Configuration.setValue(Configuration.ENTITY_SERIALIZER_CLASS, "org.jminor.framework.plugins.json.EntityJSONParser");
    assertNotNull(EntityUtil.getEntitySerializer());
    Configuration.setValue(Configuration.ENTITY_SERIALIZER_CLASS, null);
  }

  @Test
  public void getEntityDeserializer() {
    Configuration.setValue(Configuration.ENTITY_DESERIALIZER_CLASS, "org.jminor.framework.plugins.json.EntityJSONParser");
    assertNotNull(EntityUtil.getEntityDeserializer());
    Configuration.setValue(Configuration.ENTITY_DESERIALIZER_CLASS, null);
  }
}
