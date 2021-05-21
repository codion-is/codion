/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.ui;

import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.framework.demos.empdept.domain.EmpDept.Employee;
import is.codion.swing.common.ui.layout.FlexibleGridLayout.FixColumnWidths;
import is.codion.swing.common.ui.layout.FlexibleGridLayout.FixRowHeights;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityComboBox;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

import static is.codion.swing.common.ui.Components.transferFocusOnEnter;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.flexibleGridLayout;
import static is.codion.swing.common.ui.textfield.TextFields.getPreferredTextFieldSize;

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

    textFieldBuilder(Employee.NAME)
            .columns(8)
            .upperCase()
            .build();
    valueListComboBoxBuilder(Employee.JOB)
            .build();
    foreignKeyComboBoxBuilder(Employee.MGR_FK)
            .preferredSize(getPreferredTextFieldSize())
            .build();
    textFieldBuilder(Employee.SALARY)
            .build();
    textFieldBuilder(Employee.COMMISSION)
            .build();
    temporalInputPanelBuilder(Employee.HIREDATE)
            .calendarButton(true)
            .build();

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
    final EntityComboBox departmentBox = foreignKeyComboBoxBuilder(Employee.DEPARTMENT_FK).build();
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