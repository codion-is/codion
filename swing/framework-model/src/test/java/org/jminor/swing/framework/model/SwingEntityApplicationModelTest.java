/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.model.AbstractEntityApplicationModelTest;
import org.jminor.framework.model.TestDomain;

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
