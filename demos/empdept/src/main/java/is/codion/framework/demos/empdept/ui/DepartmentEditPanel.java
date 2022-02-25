/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
    setInitialFocusAttribute(Department.ID);

    JTextField departmentIdField = createTextField(Department.ID)
            .build();
    createTextField(Department.NAME)
            .upperCase(true);
    createTextField(Department.LOCATION)
            .upperCase(true);

    //we don't allow editing of the department number since it's a primary key
    getEditModel().getPrimaryKeyNullObserver().addListener(() -> {
      if (getEditModel().isEntityNew()) {
        departmentIdField.setEnabled(true);
        setInitialFocusAttribute(Department.ID);
      }
      else {
        departmentIdField.setEnabled(false);
        setInitialFocusAttribute(Department.NAME);
      }
    });

    setLayout(gridLayout(3, 1));

    addInputPanel(Department.ID);
    addInputPanel(Department.NAME);
    addInputPanel(Department.LOCATION);
  }
}
// end::initializeUI[]