/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.tools.ui;

import org.jminor.common.User;
import org.jminor.common.model.CancelException;
import org.jminor.swing.common.tools.LoadTestModel;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class LoadTestPanelTest {

  @Test
  public void test() {
    final LoadTestModel model = new TestLoadTestModel(new User("test", "hello"), 50, 2, 2, 1000);
    final LoadTestPanel panel = new LoadTestPanel(model);
    assertEquals(model, panel.getModel());
    model.exit();
  }

  @Test(expected = NullPointerException.class)
  public void constructorNullModel() {
    new LoadTestPanel(null);
  }

  private static final class TestLoadTestModel extends LoadTestModel {

    public TestLoadTestModel(final User user, final int maximumThinkTime, final int loginDelayFactor, final int applicationBatchSize,
                      final int warningTime) {
      super(user, Collections.emptyList(), maximumThinkTime, loginDelayFactor, applicationBatchSize, warningTime);
    }

    @Override
    protected Object initializeApplication() throws CancelException {
      return new Object();
    }

    @Override
    protected void disconnectApplication(final Object application) {}
  }
}
