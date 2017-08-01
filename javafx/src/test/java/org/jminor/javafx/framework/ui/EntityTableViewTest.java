/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.javafx.framework.model.FXEntityListModel;

import javafx.embed.swing.JFXPanel;
import org.junit.Test;

public final class EntityTableViewTest {

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger")), Databases.getInstance());

  static {
    new JFXPanel();
    TestDomain.init();
  }

  @Test
  public void constructor() {
    final FXEntityListModel listModel = new FXEntityListModel(TestDomain.T_EMP, CONNECTION_PROVIDER);
    new EntityTableView(listModel);
  }
}
