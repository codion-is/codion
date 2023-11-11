/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.ui;

import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.framework.demos.empdept.domain.EmpDept.Employee;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.component.EntityComboBox;

import javax.swing.JPanel;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

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
    String hiredatePattern = editModel().entityDefinition().columns().definition(Employee.HIREDATE).dateTimePattern();
    DateTimeFormatter hiredateFormatter = new DateTimeFormatterBuilder()
            .appendPattern(hiredatePattern.substring(0, hiredatePattern.length() - 2))
            .appendValueReduced(ChronoField.YEAR, 2, 2, 1940)
            .toFormatter();
    createLocalDateField(Employee.HIREDATE)
            .columns(6)
            .dateTimeFormatter(hiredateFormatter);

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

    setComponent(Employee.DEPARTMENT_FK, departmentNumberField);

    return Components.borderLayoutPanel()
            .westComponent(departmentNumberField)
            .centerComponent(departmentBox)
            .build();
  }
}
// end::initializeUI[]