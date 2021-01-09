/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.ui.explorer;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.swing.framework.tools.explorer.DatabaseExplorerModel;

import org.junit.jupiter.api.Test;

public class DatabaseExplorerPanelTest {

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  @Test
  public void test() throws ClassNotFoundException, DatabaseException {
    final DatabaseExplorerModel model = new DatabaseExplorerModel(DatabaseFactory.getDatabase(), UNIT_TEST_USER);
    new DatabaseExplorerPanel(model);
    model.getSchemaModel().refresh();
    model.getSchemaModel().sort();
    model.getSchemaModel().getSelectionModel().setSelectedIndex(0);
    model.populateSelected(schema -> {});
    model.getDefinitionModel().getSelectionModel().setSelectedIndex(0);
  }
}
