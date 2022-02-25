/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.tools.ui.loadtest;

import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.swing.common.tools.loadtest.LoadTestModel;

import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LoadTestPanelTest {

  @Test
  void test() {
    TestLoadTestModel model = new TestLoadTestModel(User.user("test", "hello".toCharArray()), 50, 2, 2);
    LoadTestPanel<Object> panel = new LoadTestPanel<Object>(model);
    assertEquals(model, panel.getModel());
    model.shutdown();
  }

  @Test
  void constructorNullModel() {
    assertThrows(NullPointerException.class, () -> new LoadTestPanel<TestLoadTestModel>(null));
  }

  private static final class TestLoadTestModel extends LoadTestModel<Object> {

    public TestLoadTestModel(final User user, final int maximumThinkTime, final int loginDelayFactor,
                             final int applicationBatchSize) {
      super(user, emptyList(), maximumThinkTime, loginDelayFactor, applicationBatchSize);
    }

    @Override
    protected Object initializeApplication() throws CancelException {
      return new Object();
    }

    @Override
    protected void disconnectApplication(final Object application) {}
  }
}
