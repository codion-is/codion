/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.framework.tools.ui.generator;

import dev.codion.common.db.exception.DatabaseException;
import dev.codion.common.user.User;
import dev.codion.common.user.Users;
import dev.codion.swing.framework.tools.generator.EntityGeneratorModel;

import org.junit.jupiter.api.Test;

public class EntityGeneratorPanelTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("jminor.test.user", "scott:tiger"));

  @Test
  public void test() throws ClassNotFoundException, DatabaseException {
    final EntityGeneratorModel model = new EntityGeneratorModel(UNIT_TEST_USER, "PETSTORE");
    new EntityGeneratorPanel(model);
    model.getTableModel().getSelectionModel().setSelectedIndex(0);
  }
}
