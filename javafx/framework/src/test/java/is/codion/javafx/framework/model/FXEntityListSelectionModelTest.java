/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.model;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.test.TestDomain;
import is.codion.framework.model.test.TestDomain.Employee;
import is.codion.javafx.framework.ui.EntityTableView;

import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxToolkit;

import static org.junit.jupiter.api.Assertions.*;

public final class FXEntityListSelectionModelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domain(new TestDomain())
          .user(UNIT_TEST_USER)
          .build();

  @BeforeAll
  public static void setUp() throws Exception {
    FxToolkit.registerPrimaryStage();
  }

  @Test
  void selectionMode() {
    FXEntityListModel list = new FXEntityListModel(Employee.TYPE, CONNECTION_PROVIDER);
    new EntityTableView(list);
    FXEntityListSelectionModel selectionModel = list.selectionModel();
    MultipleSelectionModel<Entity> multipleSelectionModel = (MultipleSelectionModel<Entity>) selectionModel.selectionModel();
    assertFalse(selectionModel.singleSelectionModeState().get());
    multipleSelectionModel.setSelectionMode(SelectionMode.SINGLE);
    assertTrue(selectionModel.singleSelectionModeState().get());
    multipleSelectionModel.setSelectionMode(SelectionMode.MULTIPLE);
    assertFalse(selectionModel.singleSelectionModeState().get());
    selectionModel.singleSelectionModeState().set(true);
    assertEquals(SelectionMode.SINGLE, multipleSelectionModel.getSelectionMode());
    selectionModel.singleSelectionModeState().set(false);
    assertEquals(SelectionMode.MULTIPLE, multipleSelectionModel.getSelectionMode());
  }
}
