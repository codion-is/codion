/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.model.AbstractEntityApplicationModelTest;
import org.jminor.framework.model.TestDomain;

import org.junit.BeforeClass;

public final class SwingEntityApplicationModelTest
        extends AbstractEntityApplicationModelTest<SwingEntityModel, SwingEntityEditModel, SwingEntityTableModel> {

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger")), Databases.getInstance());

  @BeforeClass
  public static void setUp() {
    TestDomain.init();
  }

  @Override
  protected SwingEntityModel createDepartmentModel() {
    return new DeptModel(CONNECTION_PROVIDER);
  }

  private static class DeptModel extends SwingEntityModel {
    private DeptModel(final EntityConnectionProvider connectionProvider) {
      super(TestDomain.T_DEPARTMENT, connectionProvider);
      addDetailModel(new SwingEntityModel(TestDomain.T_EMP, connectionProvider));
    }
  }
}
