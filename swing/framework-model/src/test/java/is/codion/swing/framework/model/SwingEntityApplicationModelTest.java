/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.model.tests.AbstractEntityApplicationModelTest;
import is.codion.framework.model.tests.TestDomain;

public final class SwingEntityApplicationModelTest
        extends AbstractEntityApplicationModelTest<SwingEntityModel, SwingEntityEditModel, SwingEntityTableModel> {

  @Override
  protected SwingEntityModel createDepartmentModel() {
    return new DeptModel(getConnectionProvider());
  }

  private static class DeptModel extends SwingEntityModel {
    private DeptModel(final EntityConnectionProvider connectionProvider) {
      super(TestDomain.T_DEPARTMENT, connectionProvider);
      addDetailModel(new SwingEntityModel(TestDomain.T_EMP, connectionProvider));
    }
  }
}
