/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.ui;

import is.codion.framework.demos.empdept.domain.EmpDept;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JTextField;
import java.awt.GridLayout;

// tag::constructor[]
public class DepartmentEditPanel extends EntityEditPanel {

  public DepartmentEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }
  // end::constructor[]

  // tag::initializeUI[]
  @Override
  protected void initializeUI() {
    setInitialFocusProperty(EmpDept.DEPARTMENT_ID);

    final JTextField departmentIdField = createTextField(EmpDept.DEPARTMENT_ID);
    departmentIdField.setColumns(10);
    TextFields.makeUpperCase(createTextField(EmpDept.DEPARTMENT_NAME));
    TextFields.makeUpperCase(createTextField(EmpDept.DEPARTMENT_LOCATION));

    //we don't allow editing of the department number since it's a primary key
    getEditModel().getPrimaryKeyNullObserver().addListener(() -> {
      if (getEditModel().isEntityNew()) {
        departmentIdField.setEnabled(true);
        setInitialFocusProperty(EmpDept.DEPARTMENT_ID);
      }
      else {
        departmentIdField.setEnabled(false);
        setInitialFocusProperty(EmpDept.DEPARTMENT_NAME);
      }
    });

    setLayout(new GridLayout(3, 1, 5, 5));

    addPropertyPanel(EmpDept.DEPARTMENT_ID);
    addPropertyPanel(EmpDept.DEPARTMENT_NAME);
    addPropertyPanel(EmpDept.DEPARTMENT_LOCATION);
  }
}
// end::initializeUI[]