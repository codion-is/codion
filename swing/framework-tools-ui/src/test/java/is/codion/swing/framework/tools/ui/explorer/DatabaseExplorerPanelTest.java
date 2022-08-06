/*
 * Copyright (c) 2012 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.ui.explorer;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.swing.framework.tools.explorer.DatabaseExplorerModel;

import org.junit.jupiter.api.Test;

public class DatabaseExplorerPanelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  @Test
  void test() throws ClassNotFoundException, DatabaseException {
    DatabaseExplorerModel model = new DatabaseExplorerModel(Database.instance(), UNIT_TEST_USER);
    new DatabaseExplorerPanel(model);
    model.schemaModel().refresh();
    model.schemaModel().sort();
    model.schemaModel().selectionModel().setSelectedIndex(2);
    model.populateSelected(schema -> {});
    model.definitionModel().selectionModel().setSelectedIndex(0);
  }
}
