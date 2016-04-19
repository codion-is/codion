/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.javafx;

import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.javafx.framework.model.FXEntityEditModel;
import org.jminor.javafx.framework.ui.EntityEditView;

import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public final class DepartmentEditView extends EntityEditView {

  public DepartmentEditView(final FXEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected Node initializeEditPanel() {
    setInitialFocusProperty(EmpDept.DEPARTMENT_ID);

    final TextField id = createTextField(EmpDept.DEPARTMENT_ID);
    final TextField name = createTextField(EmpDept.DEPARTMENT_NAME);
    final TextField location = createTextField(EmpDept.DEPARTMENT_LOCATION);

    final GridPane gridPane = new GridPane();

    gridPane.add(createLabel(EmpDept.DEPARTMENT_ID), 0, 0);
    gridPane.add(id, 0, 1);
    gridPane.add(createLabel(EmpDept.DEPARTMENT_NAME), 0, 2);
    gridPane.add(name, 0, 3);
    gridPane.add(createLabel(EmpDept.DEPARTMENT_LOCATION), 0, 4);
    gridPane.add(location, 0, 5);

    return gridPane;
  }
}
