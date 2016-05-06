/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.javafx;

import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entity;
import org.jminor.javafx.framework.model.FXEntityEditModel;
import org.jminor.javafx.framework.ui.EntityEditView;

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public final class EmployeeEditView extends EntityEditView {

  public EmployeeEditView(final FXEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected Node initializeEditPanel() {
    setInitialFocusProperty(EmpDept.EMPLOYEE_ID);

    final TextField id = createTextField(EmpDept.EMPLOYEE_ID);
    final TextField name = createTextField(EmpDept.EMPLOYEE_NAME);
    final TextField location = createTextField(EmpDept.EMPLOYEE_SALARY);
    final ComboBox<Entity> department = createForeignKeyComboBox(EmpDept.EMPLOYEE_DEPARTMENT_FK);
    final DatePicker picker = createDatePicker(EmpDept.EMPLOYEE_HIREDATE);
    final ComboBox<Entity> manager = createForeignKeyComboBox(EmpDept.EMPLOYEE_MGR_FK);

    final GridPane gridPane = new GridPane();

    gridPane.add(createPropertyPanel(EmpDept.EMPLOYEE_ID), 0, 0);
    gridPane.add(createPropertyPanel(EmpDept.EMPLOYEE_NAME), 0, 1);
    gridPane.add(createPropertyPanel(EmpDept.EMPLOYEE_SALARY), 0, 2);
    gridPane.add(createPropertyPanel(EmpDept.EMPLOYEE_DEPARTMENT_FK), 0, 3);
    gridPane.add(createPropertyPanel(EmpDept.EMPLOYEE_HIREDATE), 0, 4);
    gridPane.add(createPropertyPanel(EmpDept.EMPLOYEE_MGR_FK), 0, 5);

    return gridPane;
  }
}
