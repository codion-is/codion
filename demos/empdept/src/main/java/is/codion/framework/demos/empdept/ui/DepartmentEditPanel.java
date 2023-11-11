/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.ui;

import is.codion.framework.demos.empdept.domain.EmpDept.Department;
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
    initialFocusAttribute().set(Department.DEPTNO);

    createTextField(Department.DEPTNO)
            .columns(3);
    createTextField(Department.NAME)
            .columns(8);
    createTextField(Department.LOCATION)
            .columns(12);

    //we don't allow editing of the department number since it's a primary key
    editModel().primaryKeyNull().addListener(() -> {
      if (editModel().exists().get()) {
        component(Department.DEPTNO).setEnabled(false);
        initialFocusAttribute().set(Department.NAME);
      }
      else {
        component(Department.DEPTNO).setEnabled(true);
        initialFocusAttribute().set(Department.DEPTNO);
      }
    });

    setLayout(borderLayout());
    add(borderLayoutPanel()
            .northComponent(borderLayoutPanel()
                    .westComponent(createInputPanel(Department.DEPTNO))
                    .centerComponent(createInputPanel(Department.NAME))
                    .build())
            .centerComponent(createInputPanel(Department.LOCATION))
            .build(), borderLayout().CENTER);
  }
}
// end::initializeUI[]