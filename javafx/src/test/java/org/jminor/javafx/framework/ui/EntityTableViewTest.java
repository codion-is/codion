/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.framework.db.EntityConnectionProvidersTest;
import org.jminor.framework.domain.TestDomain;
import org.jminor.javafx.framework.model.FXEntityListModel;

import javafx.embed.swing.JFXPanel;
import org.junit.Test;

public final class EntityTableViewTest {

  static {
    new JFXPanel();
    TestDomain.init();
  }

  @Test
  public void constructor() {
    final FXEntityListModel listModel = new FXEntityListModel(TestDomain.T_EMP, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    new EntityTableView(listModel);
  }
}
