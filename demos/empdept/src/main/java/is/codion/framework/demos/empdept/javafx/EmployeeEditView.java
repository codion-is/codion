/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.javafx;

import is.codion.framework.demos.empdept.domain.EmpDept;
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
    setInitialFocusProperty(EmpDept.EMPLOYEE_ID);

    createTextField(EmpDept.EMPLOYEE_ID);
    createTextField(EmpDept.EMPLOYEE_NAME);
    createValueListComboBox(EmpDept.EMPLOYEE_JOB);
    createTextField(EmpDept.EMPLOYEE_SALARY);
    createForeignKeyComboBox(EmpDept.EMPLOYEE_DEPARTMENT_FK);
    createDatePicker(EmpDept.EMPLOYEE_HIREDATE);
    createForeignKeyComboBox(EmpDept.EMPLOYEE_MGR_FK);

    final GridPane gridPane = new GridPane();

    gridPane.add(createPropertyPanel(EmpDept.EMPLOYEE_ID), 0, 0);
    gridPane.add(createPropertyPanel(EmpDept.EMPLOYEE_NAME), 0, 1);
    gridPane.add(createPropertyPanel(EmpDept.EMPLOYEE_JOB), 0, 2);
    gridPane.add(createPropertyPanel(EmpDept.EMPLOYEE_SALARY), 0, 3);
    gridPane.add(createPropertyPanel(EmpDept.EMPLOYEE_DEPARTMENT_FK), 0, 4);
    gridPane.add(createPropertyPanel(EmpDept.EMPLOYEE_HIREDATE), 0, 5);
    gridPane.add(createPropertyPanel(EmpDept.EMPLOYEE_MGR_FK), 0, 6);

    return gridPane;
  }
}
