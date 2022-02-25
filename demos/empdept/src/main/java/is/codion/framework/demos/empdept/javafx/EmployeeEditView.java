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

  public EmployeeEditView(final FXEntityEditModel editModel) {
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

    gridPane.add(createPropertyPanel(Employee.ID), 0, 0);
    gridPane.add(createPropertyPanel(Employee.NAME), 0, 1);
    gridPane.add(createPropertyPanel(Employee.JOB), 0, 2);
    gridPane.add(createPropertyPanel(Employee.SALARY), 0, 3);
    gridPane.add(createPropertyPanel(Employee.DEPARTMENT_FK), 0, 4);
    gridPane.add(createPropertyPanel(Employee.HIREDATE), 0, 5);
    gridPane.add(createPropertyPanel(Employee.MGR_FK), 0, 6);

    return gridPane;
  }
}
