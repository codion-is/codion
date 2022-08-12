/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.javafx;

import is.codion.framework.demos.empdept.domain.EmpDept.Employee;
import is.codion.javafx.framework.model.FXEntityEditModel;
import is.codion.javafx.framework.ui.EntityEditView;

import javafx.scene.Node;
import javafx.scene.layout.GridPane;

public final class EmployeeEditView extends EntityEditView {

  public EmployeeEditView(FXEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected Node initializeEditPanel() {
    setInitialFocusAttribute(Employee.ID);

    createTextField(Employee.ID);
    createTextField(Employee.NAME);
    createItemComboBox(Employee.JOB);
    createTextField(Employee.SALARY);
    createForeignKeyComboBox(Employee.DEPARTMENT_FK);
    createDatePicker(Employee.HIREDATE);
    createForeignKeyComboBox(Employee.MGR_FK);

    GridPane gridPane = new GridPane();

    gridPane.add(createInputPanel(Employee.ID), 0, 0);
    gridPane.add(createInputPanel(Employee.NAME), 0, 1);
    gridPane.add(createInputPanel(Employee.JOB), 0, 2);
    gridPane.add(createInputPanel(Employee.SALARY), 0, 3);
    gridPane.add(createInputPanel(Employee.DEPARTMENT_FK), 0, 4);
    gridPane.add(createInputPanel(Employee.HIREDATE), 0, 5);
    gridPane.add(createInputPanel(Employee.MGR_FK), 0, 6);

    return gridPane;
  }
}
