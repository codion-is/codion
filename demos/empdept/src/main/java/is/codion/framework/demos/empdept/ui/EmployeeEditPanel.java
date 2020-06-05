/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.ui;

import is.codion.framework.demos.empdept.domain.EmpDept.Employee;
import is.codion.framework.domain.entity.Entity;
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
    setInitialFocusAttribute(Employee.NAME);

    final JTextField nameField = TextFields.upperCase(createTextField(Employee.NAME));
    nameField.setColumns(8);
    createValueListComboBox(Employee.JOB);
    final JComboBox<Entity> managerBox = createForeignKeyComboBox(Employee.MGR_FK);
    managerBox.setPreferredSize(TextFields.getPreferredTextFieldSize());
    createForeignKeyComboBox(Employee.DEPARTMENT_FK);
    createTextField(Employee.SALARY);
    createTextField(Employee.COMMISSION);
    createTemporalInputPanel(Employee.HIREDATE, CalendarButton.YES);

    setLayout(new FlexibleGridLayout(3, 3, 5, 5, FixRowHeights.YES, FixColumnWidths.NO));

    addInputPanel(Employee.NAME);
    addInputPanel(Employee.JOB);
    addInputPanel(Employee.DEPARTMENT_FK);

    addInputPanel(Employee.MGR_FK);
    addInputPanel(Employee.SALARY);
    addInputPanel(Employee.COMMISSION);

    addInputPanel(Employee.HIREDATE);
    add(new JLabel());
    add(new JLabel());
  }
}
// end::initializeUI[]