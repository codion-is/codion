/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.ui;

import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.framework.demos.empdept.domain.EmpDept.Employee;
import is.codion.swing.common.ui.layout.FlexibleGridLayout.FixColumnWidths;
import is.codion.swing.common.ui.layout.FlexibleGridLayout.FixRowHeights;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.common.ui.time.TemporalInputPanel.CalendarButton;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityComboBox;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;

import static is.codion.swing.common.ui.Components.transferFocusOnEnter;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.flexibleGridLayout;

// tag::constructor[]
public class EmployeeEditPanel extends EntityEditPanel {

  public EmployeeEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }
  // end::constructor[]

  // tag::initializeUI[]
  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Employee.NAME);

    final JTextField nameField = TextFields.upperCase(createTextField(Employee.NAME));
    nameField.setColumns(8);
    createValueListComboBox(Employee.JOB);
    final EntityComboBox managerBox = createForeignKeyComboBox(Employee.MGR_FK);
    managerBox.setPreferredSize(TextFields.getPreferredTextFieldSize());
    createTextField(Employee.SALARY);
    createTextField(Employee.COMMISSION);
    createTemporalInputPanel(Employee.HIREDATE, CalendarButton.YES);

    setLayout(flexibleGridLayout(3, 3, FixRowHeights.YES, FixColumnWidths.NO));

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
    final EntityComboBox departmentBox = createForeignKeyComboBox(Employee.DEPARTMENT_FK);
    final IntegerField departmentIdField = departmentBox.integerFieldSelector(Department.ID);
    transferFocusOnEnter(departmentIdField);

    final JPanel departmentPanel = new JPanel(borderLayout());
    departmentPanel.add(departmentIdField, BorderLayout.WEST);
    departmentPanel.add(departmentBox, BorderLayout.CENTER);

    setComponent(Employee.DEPARTMENT_FK, departmentIdField);

    return departmentPanel;
  }
}
// end::initializeUI[]