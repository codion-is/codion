/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.model.EntityTableModel;
import org.jminor.swing.framework.model.SwingEntityTableModel;

import org.junit.BeforeClass;
import org.junit.Test;

public class EntityConditionPanelTest {

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger")), Databases.getInstance());

  @BeforeClass
  public static void setUp() {
    TestDomain.init();
  }

  @Test
  public void test() {
    final EntityTableModel tableModel = new SwingEntityTableModel(TestDomain.T_DEPARTMENT, CONNECTION_PROVIDER);
    new EntityConditionPanel(tableModel);
  }
}
