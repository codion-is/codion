/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.tools;

import org.jminor.common.model.User;
import org.jminor.common.model.tools.LoadTestModel;
import org.jminor.common.model.tools.LoadTestModelTest;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LoadTestPanelTest {

  @Test
  public void test() {
    final LoadTestModel model = new LoadTestModelTest.TestLoadTestModel(new User("test", "hello"), 50, 2, 2, 1000);
    final LoadTestPanel panel = new LoadTestPanel(model);
    assertEquals(model, panel.getModel());
    model.exit();
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullModel() {
    new LoadTestPanel(null);
  }
}
