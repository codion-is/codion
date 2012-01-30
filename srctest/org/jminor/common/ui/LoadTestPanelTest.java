package org.jminor.common.ui;

import org.jminor.common.model.LoadTestModel;
import org.jminor.common.model.LoadTestModelTest;
import org.jminor.common.model.User;

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
