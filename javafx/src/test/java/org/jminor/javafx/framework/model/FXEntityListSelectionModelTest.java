/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.common.db.Databases;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.model.TestDomain;
import org.jminor.javafx.framework.ui.EntityTableView;

import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxToolkit;

import static org.junit.jupiter.api.Assertions.*;

public final class FXEntityListSelectionModelTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("jminor.test.user", "scott:tiger"));

  protected static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          Databases.getInstance()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  @BeforeAll
  public static void setUp() throws Exception {
    FxToolkit.registerPrimaryStage();
  }

  @Test
  public void selectionMode() {
    final FXEntityListModel list = new FXEntityListModel(TestDomain.T_EMP, CONNECTION_PROVIDER);
    new EntityTableView(list);
    final FXEntityListSelectionModel selectionModel = list.getSelectionModel();
    final MultipleSelectionModel<Entity> multipleSelectionModel = (MultipleSelectionModel<Entity>) selectionModel.getSelectionModel();
    assertFalse(selectionModel.getSingleSelectionModeState().get());
    multipleSelectionModel.setSelectionMode(SelectionMode.SINGLE);
    assertTrue(selectionModel.getSingleSelectionModeState().get());
    multipleSelectionModel.setSelectionMode(SelectionMode.MULTIPLE);
    assertFalse(selectionModel.getSingleSelectionModeState().get());
    selectionModel.getSingleSelectionModeState().set(true);
    assertEquals(SelectionMode.SINGLE, multipleSelectionModel.getSelectionMode());
    selectionModel.getSingleSelectionModeState().set(false);
    assertEquals(SelectionMode.MULTIPLE, multipleSelectionModel.getSelectionMode());
  }
}
