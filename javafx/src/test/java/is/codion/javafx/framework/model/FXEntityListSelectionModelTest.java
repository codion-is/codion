/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.model;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.test.TestDomain;
import is.codion.javafx.framework.ui.EntityTableView;

import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxToolkit;

import static org.junit.jupiter.api.Assertions.*;

public final class FXEntityListSelectionModelTest {

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          DatabaseFactory.getDatabase()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  @BeforeAll
  public static void setUp() throws Exception {
    FxToolkit.registerPrimaryStage();
  }

  @Test
  void selectionMode() {
    FXEntityListModel list = new FXEntityListModel(TestDomain.T_EMP, CONNECTION_PROVIDER);
    new EntityTableView(list);
    FXEntityListSelectionModel selectionModel = list.getSelectionModel();
    MultipleSelectionModel<Entity> multipleSelectionModel = (MultipleSelectionModel<Entity>) selectionModel.getSelectionModel();
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
