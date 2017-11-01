/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans.ui;

import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.layout.FlexibleGridLayout;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class EmployeeEditPanel extends EntityEditPanel {

  public EmployeeEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    final JTextField txtName = (JTextField) UiUtil.makeUpperCase(createTextField(EmpDept.EMPLOYEE_NAME));
    createValueListComboBox(EmpDept.EMPLOYEE_JOB);
    final JComboBox boxManager = createForeignKeyComboBox(EmpDept.EMPLOYEE_MGR_FK);
    createForeignKeyComboBox(EmpDept.EMPLOYEE_DEPARTMENT_FK);
    createTextField(EmpDept.EMPLOYEE_SALARY);
    createTextField(EmpDept.EMPLOYEE_COMMISSION);
    createDateInputPanel(EmpDept.EMPLOYEE_HIREDATE, true);

    setInitialFocusComponent(txtName);

    txtName.setColumns(8);
    boxManager.setPreferredSize(UiUtil.getPreferredTextFieldSize());

    setLayout(new FlexibleGridLayout(3,3,5,5,true,false));

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
