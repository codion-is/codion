/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.ui.generator;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.common.user.Users;
import is.codion.swing.framework.tools.generator.EntityGeneratorModel;

import org.junit.jupiter.api.Test;

public class EntityGeneratorPanelTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  @Test
  public void test() throws ClassNotFoundException, DatabaseException {
    final EntityGeneratorModel model = new EntityGeneratorModel(UNIT_TEST_USER);
    new EntityGeneratorPanel(model);
    model.getSchemaModel().sort();
    model.getSchemaModel().getSelectionModel().selectAll();
    model.getTableModel().getSelectionModel().setSelectedIndex(0);
  }
}
