/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

/**
 * User: Björn Darri
 * Date: 29.7.2010
 * Time: 22:47:34
 */
public class DefaultEntityValidatorTest {

  @Test
  public void test() {
    final DefaultEntityValidator validator = new DefaultEntityValidator("test", null);
    assertEquals("test", validator.getEntityID());
    assertNull(validator.getDbProvider());
  }
}
