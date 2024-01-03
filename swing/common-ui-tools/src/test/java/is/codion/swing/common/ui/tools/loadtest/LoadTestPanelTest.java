/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.tools.loadtest;

import is.codion.common.user.User;
import is.codion.swing.common.model.tools.loadtest.LoadTestModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LoadTestPanelTest {

  @Test
  void test() {
    LoadTestModel<Object> model = LoadTestModel.builder(user -> new Object(), object -> {})
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
    assertThrows(NullPointerException.class, () -> new LoadTestPanel<LoadTestModel<?>>(null));
  }
}
