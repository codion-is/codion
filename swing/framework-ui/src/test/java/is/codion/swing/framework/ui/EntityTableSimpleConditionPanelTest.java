/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityTableModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EntityTableSimpleConditionPanelTest {

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          DatabaseFactory.getDatabase()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  @Test
  void test() {
    final SwingEntityTableModel tableModel = new SwingEntityTableModel(TestDomain.T_EMP, CONNECTION_PROVIDER);
    final EntityTableSimpleConditionPanel panel = new EntityTableSimpleConditionPanel(tableModel.getTableConditionModel(),
            tableModel.getColumnModel(), tableModel::refresh);
    panel.setSearchText("BLAKE");
    panel.performSearch();
    assertEquals(1, tableModel.getRowCount());
  }
}
