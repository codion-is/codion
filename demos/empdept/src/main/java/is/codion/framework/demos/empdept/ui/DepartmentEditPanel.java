/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.ui;

import is.codion.framework.demos.empdept.domain.EmpDept.Department;
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
    setInitialFocusAttribute(Department.ID);

    final JTextField departmentIdField = createTextField(Department.ID);
    departmentIdField.setColumns(10);
    TextFields.upperCase(createTextField(Department.NAME));
    TextFields.upperCase(createTextField(Department.LOCATION));

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

    setLayout(new GridLayout(3, 1, 5, 5));

    addPropertyPanel(Department.ID);
    addPropertyPanel(Department.NAME);
    addPropertyPanel(Department.LOCATION);
  }
}
// end::initializeUI[]