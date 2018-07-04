/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.tools.ui;

import org.jminor.common.User;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.swing.framework.tools.EntityGeneratorModel;

import org.junit.jupiter.api.Test;

public class EntityGeneratorPanelTest {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  @Test
  public void test() throws ClassNotFoundException, DatabaseException {
    final EntityGeneratorModel model = new EntityGeneratorModel(UNIT_TEST_USER, "PETSTORE");
    new EntityGeneratorPanel(model);
    model.getTableModel().getSelectionModel().setSelectedIndex(0);
  }
}
