/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.framework.db.EntityConnectionProvidersTest;
import org.jminor.framework.domain.TestDomain;
import org.jminor.javafx.framework.model.FXEntityEditModel;
import org.jminor.javafx.framework.model.FXEntityListModel;
import org.jminor.javafx.framework.model.FXEntityModel;

import javafx.embed.swing.JFXPanel;
import org.junit.Test;

public final class EntityViewTest {

  static {
    new JFXPanel();
    TestDomain.init();
  }

  @Test
  public void constructor() {
    final FXEntityEditModel editModel = new FXEntityEditModel(TestDomain.T_EMP, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    final FXEntityListModel listModel = new FXEntityListModel(TestDomain.T_EMP, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    final FXEntityModel model = new FXEntityModel(editModel, listModel);

    new EntityView(model, new EntityEditViewTest.EmpEditView(editModel), new EntityTableView(listModel)).initializePanel();
  }
}
