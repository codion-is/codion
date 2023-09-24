/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson.
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
    editModel().primaryKeyNull().addListener(() -> {
      if (editModel().entityNew().get()) {
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