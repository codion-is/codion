/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.ui;

import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JTextField;

import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

// tag::constructor[]
public class DepartmentEditPanel extends EntityEditPanel {

  public DepartmentEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }
  // end::constructor[]

  // tag::initializeUI[]
  @Override
  protected void initializeUI() {
    initialFocusAttribute().set(Department.ID);

    JTextField departmentIdField = createTextField(Department.ID)
            .build();
    createTextField(Department.NAME)
            .upperCase(true);
    createTextField(Department.LOCATION)
            .upperCase(true);

    //we don't allow editing of the department number since it's a primary key
    editModel().primaryKeyNull().addListener(() -> {
      if (editModel().exists().get()) {
        departmentIdField.setEnabled(false);
        initialFocusAttribute().set(Department.NAME);
      }
      else {
        departmentIdField.setEnabled(true);
        initialFocusAttribute().set(Department.ID);
      }
    });

    setLayout(gridLayout(3, 1));

    addInputPanel(Department.ID);
    addInputPanel(Department.NAME);
    addInputPanel(Department.LOCATION);
  }
}
// end::initializeUI[]