/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.javafx;

import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.javafx.framework.model.FXEntityEditModel;
import is.codion.javafx.framework.ui.EntityEditView;

import javafx.scene.Node;
import javafx.scene.layout.GridPane;

public final class DepartmentEditView extends EntityEditView {

  public DepartmentEditView(FXEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected Node initializeEditPanel() {
    setInitialFocusAttribute(Department.ID);

    createTextField(Department.ID);
    createTextField(Department.NAME);
    createTextField(Department.LOCATION);

    GridPane gridPane = new GridPane();

    gridPane.add(createInputPanel(Department.ID), 0, 0);
    gridPane.add(createInputPanel(Department.NAME), 0, 1);
    gridPane.add(createInputPanel(Department.LOCATION), 0, 2);

    return gridPane;
  }
}
