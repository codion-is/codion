/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.tools.loadtest;

import is.codion.common.user.User;
import is.codion.swing.common.model.tools.loadtest.LoadTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LoadTestPanelTest {

  @Test
  void test() {
    LoadTest<Object> model = LoadTest.builder(user -> new Object(), object -> {})
            .user(User.user("test"))
            .minimumThinkTime(25)
            .maximumThinkTime(50)
            .loginDelayFactor(2)
            .applicationBatchSize(2)
            .build();
    LoadTestPanel<Object> panel = new LoadTestPanel<>(model);
    assertEquals(model, panel.model());
    model.shutdown();
  }

  @Test
  void constructorNullModel() {
    assertThrows(NullPointerException.class, () -> new LoadTestPanel<LoadTest<?>>(null));
  }
}
