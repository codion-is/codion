/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.ui;

import is.codion.framework.demos.empdept.domain.EmpDept;
import is.codion.swing.common.ui.layout.FlexibleGridLayout;
import is.codion.swing.common.ui.layout.FlexibleGridLayout.FixColumnWidths;
import is.codion.swing.common.ui.layout.FlexibleGridLayout.FixRowHeights;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.common.ui.time.TemporalInputPanel.CalendarButton;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

// tag::constructor[]
public class EmployeeEditPanel extends EntityEditPanel {

  public EmployeeEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }
  // end::constructor[]

  // tag::initializeUI[]
  @Override
  protected void initializeUI() {
    setInitialFocusProperty(EmpDept.EMPLOYEE_NAME);

    final JTextField nameField = TextFields.upperCase(createTextField(EmpDept.EMPLOYEE_NAME));
    nameField.setColumns(8);
    createValueListComboBox(EmpDept.EMPLOYEE_JOB);
    final JComboBox managerBox = createForeignKeyComboBox(EmpDept.EMPLOYEE_MGR_FK);
    managerBox.setPreferredSize(TextFields.getPreferredTextFieldSize());
    createForeignKeyComboBox(EmpDept.EMPLOYEE_DEPARTMENT_FK);
    createTextField(EmpDept.EMPLOYEE_SALARY);
    createTextField(EmpDept.EMPLOYEE_COMMISSION);
    createTemporalInputPanel(EmpDept.EMPLOYEE_HIREDATE, CalendarButton.YES);

    setLayout(new FlexibleGridLayout(3, 3, 5, 5, FixRowHeights.YES, FixColumnWidths.NO));

    addPropertyPanel(EmpDept.EMPLOYEE_NAME);
    addPropertyPanel(EmpDept.EMPLOYEE_JOB);
    addPropertyPanel(EmpDept.EMPLOYEE_DEPARTMENT_FK);

    addPropertyPanel(EmpDept.EMPLOYEE_MGR_FK);
    addPropertyPanel(EmpDept.EMPLOYEE_SALARY);
    addPropertyPanel(EmpDept.EMPLOYEE_COMMISSION);

    addPropertyPanel(EmpDept.EMPLOYEE_HIREDATE);
    add(new JLabel());
    add(new JLabel());
  }
}
// end::initializeUI[]