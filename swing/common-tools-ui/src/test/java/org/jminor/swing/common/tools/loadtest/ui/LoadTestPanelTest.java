/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.tools.loadtest.ui;

import org.jminor.common.model.CancelException;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;
import org.jminor.swing.common.tools.loadtest.LoadTestModel;

import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LoadTestPanelTest {

  @Test
  public void test() {
    final LoadTestModel model = new TestLoadTestModel(Users.user("test", "hello".toCharArray()), 50, 2, 2, 1000);
    final LoadTestPanel panel = new LoadTestPanel(model);
    assertEquals(model, panel.getModel());
    model.exit();
  }

  @Test
  public void constructorNullModel() {
    assertThrows(NullPointerException.class, () -> new LoadTestPanel(null));
  }

  private static final class TestLoadTestModel extends LoadTestModel {

    public TestLoadTestModel(final User user, final int maximumThinkTime, final int loginDelayFactor,
                             final int applicationBatchSize, final int warningTime) {
      super(user, emptyList(), maximumThinkTime, loginDelayFactor, applicationBatchSize, warningTime);
    }

    @Override
    protected Object initializeApplication() throws CancelException {
      return new Object();
    }

    @Override
    protected void disconnectApplication(final Object application) {}
  }
}
