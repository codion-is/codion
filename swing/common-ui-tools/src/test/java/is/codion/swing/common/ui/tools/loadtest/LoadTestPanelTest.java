/*
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.tools.loadtest;

import is.codion.common.model.loadtest.LoadTest;
import is.codion.common.user.User;
import is.codion.swing.common.model.tools.loadtest.LoadTestModel;

import org.junit.jupiter.api.Test;

import static is.codion.swing.common.model.tools.loadtest.LoadTestModel.loadTestModel;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LoadTestPanelTest {

  @Test
  void test() {
    LoadTest<Object> loadTest = LoadTest.builder(user -> new Object(), object -> {})
            .user(User.user("test"))
            .minimumThinkTime(25)
            .maximumThinkTime(50)
            .loginDelayFactor(2)
            .applicationBatchSize(2)
            .build();
    LoadTestModel<Object> model = loadTestModel(loadTest);
    LoadTestPanel<Object> panel = new LoadTestPanel<>(model);
    assertEquals(model, panel.model());
    loadTest.shutdown();
  }

  @Test
  void constructorNullModel() {
    assertThrows(NullPointerException.class, () -> new LoadTestPanel<LoadTest<?>>(null));
  }
}
