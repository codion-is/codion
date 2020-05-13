/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.empdept.javafx;

import dev.codion.framework.demos.empdept.domain.EmpDept;
import dev.codion.javafx.framework.model.FXEntityEditModel;
import dev.codion.javafx.framework.ui.EntityEditView;

import javafx.scene.Node;
import javafx.scene.layout.GridPane;

public final class DepartmentEditView extends EntityEditView {

  public DepartmentEditView(final FXEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected Node initializeEditPanel() {
    setInitialFocusProperty(EmpDept.DEPARTMENT_ID);

    createTextField(EmpDept.DEPARTMENT_ID);
    createTextField(EmpDept.DEPARTMENT_NAME);
    createTextField(EmpDept.DEPARTMENT_LOCATION);

    final GridPane gridPane = new GridPane();

    gridPane.add(createPropertyPanel(EmpDept.DEPARTMENT_ID), 0, 0);
    gridPane.add(createPropertyPanel(EmpDept.DEPARTMENT_NAME), 0, 1);
    gridPane.add(createPropertyPanel(EmpDept.DEPARTMENT_LOCATION), 0, 2);

    return gridPane;
  }
}
