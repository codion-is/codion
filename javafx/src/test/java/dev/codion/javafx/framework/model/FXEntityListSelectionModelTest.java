/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.javafx.framework.model;

import dev.codion.common.db.database.Databases;
import dev.codion.common.user.User;
import dev.codion.common.user.Users;
import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.db.local.LocalEntityConnectionProvider;
import dev.codion.framework.domain.entity.Entity;
import dev.codion.framework.model.tests.TestDomain;
import dev.codion.javafx.framework.ui.EntityTableView;

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
