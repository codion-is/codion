/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.javafx;

import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.javafx.framework.model.FXEntityEditModel;
import org.jminor.javafx.framework.ui.EntityEditView;

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
