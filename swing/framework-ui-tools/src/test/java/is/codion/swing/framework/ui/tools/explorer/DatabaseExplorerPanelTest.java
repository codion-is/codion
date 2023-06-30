/*
 * Copyright (c) 2012 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.tools.explorer;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.swing.framework.model.tools.explorer.DatabaseExplorerModel;

import org.junit.jupiter.api.Test;

public class DatabaseExplorerPanelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  @Test
  void test() throws ClassNotFoundException, DatabaseException {
    DatabaseExplorerModel model = DatabaseExplorerModel.databaseExplorerModel(Database.instance(), UNIT_TEST_USER);
    new DatabaseExplorerPanel(model);
    model.schemaModel().refresh();
    model.schemaModel().sortItems();
    model.schemaModel().selectionModel().setSelectedIndex(2);
    model.populateSelected(schema -> {});
    model.definitionModel().selectionModel().setSelectedIndex(0);
  }
}