/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.tools.generator.ui;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.User;
import org.jminor.framework.tools.generator.EntityGeneratorModel;

import org.junit.Test;

public class EntityGeneratorPanelTest {

  @Test
  public void test() throws ClassNotFoundException, DatabaseException {
    final EntityGeneratorModel model = new EntityGeneratorModel(new User("scott", "tiger"), "PETSTORE");
    new EntityGeneratorPanel(model);
    model.getTableModel().getSelectionModel().setSelectedIndex(0);
  }
}
