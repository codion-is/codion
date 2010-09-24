/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.tools;

import org.jminor.common.model.User;
import org.jminor.framework.tools.generator.EntityGeneratorModel;

import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class EntityGeneratorModelTest {

  @Test
  public void test() throws Exception {
    final EntityGeneratorModel model = new EntityGeneratorModel(new User("scott", "tiger"), "CHINOOK");
    assertNotNull(model);
    model.exit();
  }
}
