/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.employees.ui;

import is.codion.framework.demos.employees.domain.Employees.Department;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.swing.common.ui.component.Components.borderLayoutPanel;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;

// tag::constructor[]
public class DepartmentEditPanel extends EntityEditPanel {

  public DepartmentEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }
  // end::constructor[]

  // tag::initializeUI[]
  @Override
  protected void initializeUI() {
    initialFocusAttribute().set(Department.DEPARTMENT_NO);

    createTextField(Department.DEPARTMENT_NO)
            .columns(3)
            //don't allow editing of existing department numbers
            .enabled(editModel().exists().not());
    createTextField(Department.NAME)
            .columns(8);
    createTextField(Department.LOCATION)
            .columns(12);

    editModel().exists().addDataListener(exists ->
            initialFocusAttribute().set(exists ? Department.NAME: Department.DEPARTMENT_NO));

    setLayout(borderLayout());
    add(borderLayoutPanel()
            .northComponent(borderLayoutPanel()
                    .westComponent(createInputPanel(Department.DEPARTMENT_NO))
                    .centerComponent(createInputPanel(Department.NAME))
                    .build())
            .centerComponent(createInputPanel(Department.LOCATION))
            .build(), borderLayout().CENTER);
  }
}
// end::initializeUI[]