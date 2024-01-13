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
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.employees.ui;

import is.codion.framework.demos.employees.domain.Employees.Department;
import is.codion.framework.demos.employees.domain.Employees.Employee;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.component.EntityComboBox;

import javax.swing.JPanel;

import static is.codion.swing.common.ui.component.Components.gridLayoutPanel;
import static is.codion.swing.common.ui.layout.Layouts.flexibleGridLayout;

// tag::constructor[]
public class EmployeeEditPanel extends EntityEditPanel {

  public EmployeeEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }
  // end::constructor[]

  // tag::initializeUI[]
  @Override
  protected void initializeUI() {
    initialFocusAttribute().set(Employee.NAME);

    createTextField(Employee.NAME)
            .columns(8);
    createItemComboBox(Employee.JOB);
    createForeignKeyComboBox(Employee.MGR_FK);
    createTextField(Employee.SALARY)
            .columns(5);
    createTextField(Employee.COMMISSION)
            .columns(5);
    createTemporalFieldPanel(Employee.HIREDATE)
            .columns(6);

    setLayout(flexibleGridLayout(0, 3));

    addInputPanel(Employee.NAME);
    addInputPanel(Employee.DEPARTMENT_FK, createDepartmentPanel());
    addInputPanel(Employee.JOB);

    addInputPanel(Employee.MGR_FK);
    add(gridLayoutPanel(1, 2)
            .add(createInputPanel(Employee.SALARY))
            .add(createInputPanel(Employee.COMMISSION))
            .build());
    addInputPanel(Employee.HIREDATE);
  }

  private JPanel createDepartmentPanel() {
    EntityComboBox departmentBox = createForeignKeyComboBox(Employee.DEPARTMENT_FK).build();
    NumberField<Integer> departmentNumberField = departmentBox.integerSelectorField(Department.DEPTNO)
            .transferFocusOnEnter(true)
            .build();

    component(Employee.DEPARTMENT_FK).set(departmentNumberField);

    return Components.borderLayoutPanel()
            .westComponent(departmentNumberField)
            .centerComponent(departmentBox)
            .build();
  }
}
// end::initializeUI[]