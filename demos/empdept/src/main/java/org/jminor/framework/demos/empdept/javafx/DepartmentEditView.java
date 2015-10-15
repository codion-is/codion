/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.javafx;

import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.javafx.framework.model.EntityEditModel;
import org.jminor.javafx.framework.ui.EntityEditView;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public final class DepartmentEditView extends EntityEditView {

  public DepartmentEditView(final EntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected Node initializeEditPanel() {
    TextField id = createIntegerField(EmpDept.DEPARTMENT_ID);
    TextField name = createTextField(EmpDept.DEPARTMENT_NAME);
    TextField location = createTextField(EmpDept.DEPARTMENT_LOCATION);

    GridPane gridPane = new GridPane();

    gridPane.add(new Label("Id"), 0, 0);
    gridPane.add(id, 0, 1);
    gridPane.add(new Label("Name"), 0, 2);
    gridPane.add(name, 0, 3);
    gridPane.add(new Label("Location"), 0, 4);
    gridPane.add(location, 0, 5);

    return gridPane;
  }
}
