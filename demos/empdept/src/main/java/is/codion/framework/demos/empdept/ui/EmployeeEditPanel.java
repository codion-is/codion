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
import is.codion.framework.demos.empdept.domain.EmpDept.Employee;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.component.EntityComboBox;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

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
            .columns(8)
            .upperCase(true);
    createItemComboBox(Employee.JOB);
    createForeignKeyComboBox(Employee.MGR_FK);
    createTextField(Employee.SALARY);
    createTextField(Employee.COMMISSION);
    String hiredatePattern = editModel().entityDefinition().columns().definition(Employee.HIREDATE).dateTimePattern();
    DateTimeFormatter hiredateFormatter = new DateTimeFormatterBuilder()
            .appendPattern(hiredatePattern.substring(0, hiredatePattern.length() - 2))
            .appendValueReduced(ChronoField.YEAR, 2, 2, 1940)
            .toFormatter();
    createLocalDateField(Employee.HIREDATE)
            .dateTimeFormatter(hiredateFormatter);

    setLayout(flexibleGridLayout(3, 3));

    addInputPanel(Employee.NAME);
    addInputPanel(Employee.JOB);
    addInputPanel(Employee.DEPARTMENT_FK, createDepartmentPanel());

    addInputPanel(Employee.MGR_FK);
    addInputPanel(Employee.SALARY);
    addInputPanel(Employee.COMMISSION);

    addInputPanel(Employee.HIREDATE);
    add(new JLabel());
    add(new JLabel());
  }

  private JPanel createDepartmentPanel() {
    EntityComboBox departmentBox = createForeignKeyComboBox(Employee.DEPARTMENT_FK).build();
    NumberField<Integer> departmentIdField = departmentBox.integerSelectorField(Department.ID)
            .transferFocusOnEnter(true)
            .build();

    setComponent(Employee.DEPARTMENT_FK, departmentIdField);

    return Components.borderLayoutPanel()
            .westComponent(departmentIdField)
            .centerComponent(departmentBox)
            .build();
  }
}
// end::initializeUI[]