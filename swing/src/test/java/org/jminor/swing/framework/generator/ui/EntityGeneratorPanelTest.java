/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.generator.ui;

import org.jminor.common.User;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.swing.framework.generator.EntityGeneratorModel;

import org.junit.Test;

public class EntityGeneratorPanelTest {

  @Test
  public void test() throws ClassNotFoundException, DatabaseException {
    final EntityGeneratorModel model = new EntityGeneratorModel(User.UNIT_TEST_USER, "PETSTORE");
    new EntityGeneratorPanel(model);
    model.getTableModel().getSelectionModel().setSelectedIndex(0);
  }
}
