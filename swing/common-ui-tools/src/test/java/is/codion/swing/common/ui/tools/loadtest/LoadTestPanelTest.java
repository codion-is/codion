/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.tools.loadtest;

import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.swing.common.model.tools.loadtest.LoadTestModel;

import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LoadTestPanelTest {

  @Test
  void test() {
    TestLoadTestModel model = new TestLoadTestModel(User.user("test", "hello".toCharArray()), 50, 2, 2);
    LoadTestPanel<Object> panel = new LoadTestPanel<>(model);
    assertEquals(model, panel.model());
    model.shutdown();
  }

  @Test
  void constructorNullModel() {
    assertThrows(NullPointerException.class, () -> new LoadTestPanel<TestLoadTestModel>(null));
  }

  private static final class TestLoadTestModel extends LoadTestModel<Object> {

    public TestLoadTestModel(User user, int maximumThinkTime, int loginDelayFactor,
                             int applicationBatchSize) {
      super(user, emptyList(), maximumThinkTime, loginDelayFactor, applicationBatchSize);
    }

    @Override
    protected Object createApplication(User user) throws CancelException {
      return new Object();
    }

    @Override
    protected void disconnectApplication(Object application) {}
  }
}
