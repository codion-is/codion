/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.javafx;

import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.javafx.framework.model.EntityEditModel;
import org.jminor.javafx.framework.ui.EntityEditView;

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public final class EmployeeEditView extends EntityEditView {

  public EmployeeEditView(final EntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected Node initializeEditPanel() {
    final TextField id = createTextField(EmpDept.EMPLOYEE_ID);
    setInitialFocusControl(id);
    final TextField name = createTextField(EmpDept.EMPLOYEE_NAME);
    final TextField location = createTextField(EmpDept.EMPLOYEE_SALARY);
    final ComboBox department = createComboBox(EmpDept.EMPLOYEE_DEPARTMENT_FK);
    final DatePicker picker = createDatePicker(EmpDept.EMPLOYEE_HIREDATE);
    final ComboBox manager = createComboBox(EmpDept.EMPLOYEE_MGR_FK);

    final GridPane gridPane = new GridPane();

    gridPane.add(new Label("Id"), 0, 0);
    gridPane.add(id, 0, 1);
    gridPane.add(new Label("Name"), 0, 2);
    gridPane.add(name, 0, 3);
    gridPane.add(new Label("Salary"), 0, 4);
    gridPane.add(location, 0, 5);
    gridPane.add(new Label("Department"), 0, 6);
    gridPane.add(department, 0, 7);
    gridPane.add(new Label("Hiredate"), 0, 8);
    gridPane.add(picker, 0, 9);
    gridPane.add(new Label("Manager"), 0, 10);
    gridPane.add(manager, 0, 11);

    return gridPane;
  }
}
