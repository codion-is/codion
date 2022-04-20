/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.ui;

import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.framework.demos.empdept.domain.EmpDept.Employee;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityComboBox;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
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
    setInitialFocusAttribute(Employee.NAME);

    createTextField(Employee.NAME)
            .columns(8)
            .upperCase(true);
    createItemComboBox(Employee.JOB);
    createForeignKeyComboBox(Employee.MGR_FK);
    createTextField(Employee.SALARY);
    createTextField(Employee.COMMISSION);
    String hiredatePattern = getEditModel().getEntityDefinition().getProperty(Employee.HIREDATE).getDateTimePattern();
    DateTimeFormatter hiredateFormatter = new DateTimeFormatterBuilder()
            .appendPattern(hiredatePattern.substring(0, hiredatePattern.length() - 2))
            .appendValueReduced(ChronoField.YEAR, 2, 2, 1940)
            .toFormatter();
    createLocalDateField(Employee.HIREDATE)
            .dateTimeFormatter(hiredateFormatter);

    setLayout(flexibleGridLayout(3, 3));

    addInputPanel(Employee.NAME);
    addInputPanel(Employee.JOB);
    addInputPanel(Employee.DEPARTMENT_FK, initializeDepartmentPanel());

    addInputPanel(Employee.MGR_FK);
    addInputPanel(Employee.SALARY);
    addInputPanel(Employee.COMMISSION);

    addInputPanel(Employee.HIREDATE);
    add(new JLabel());
    add(new JLabel());
  }

  private JPanel initializeDepartmentPanel() {
    EntityComboBox departmentBox = createForeignKeyComboBox(Employee.DEPARTMENT_FK).build();
    NumberField<Integer> departmentIdField = departmentBox.integerSelectorField(Department.ID)
            .transferFocusOnEnter(true)
            .build();

    JPanel departmentPanel = new JPanel(borderLayout());
    departmentPanel.add(departmentIdField, BorderLayout.WEST);
    departmentPanel.add(departmentBox, BorderLayout.CENTER);

    setComponent(Employee.DEPARTMENT_FK, departmentIdField);

    return departmentPanel;
  }
}
// end::initializeUI[]