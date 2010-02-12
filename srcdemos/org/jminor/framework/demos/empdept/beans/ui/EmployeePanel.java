/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans.ui;

import org.jminor.common.ui.DateInputPanel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class EmployeePanel extends EntityPanel {

  public EmployeePanel(final EntityModel model) {
    super(model, EmpDept.getString(EmpDept.T_EMPLOYEE), true, true);
  }

  @Override
  protected void initialize() {
    getTablePanel().setSummaryPanelVisible(true);
  }

  @Override
  protected EntityEditPanel initializeEditPanel(final EntityEditModel editModel) {
    return new EntityEditPanel(editModel) {
      @Override
      protected void initializeUI() {
        final JTextField txtName = (JTextField) UiUtil.makeUpperCase(createTextField(EmpDept.EMPLOYEE_NAME));
        final JTextField txtJob = (JTextField) UiUtil.makeUpperCase(createTextField(EmpDept.EMPLOYEE_JOB));
        final JComboBox boxManager = createEntityComboBox(EmpDept.EMPLOYEE_MGR_FK);
        final JComboBox boxDepartment = createEntityComboBox(EmpDept.EMPLOYEE_DEPARTMENT_FK);
        final JTextField txtSalary = createTextField(EmpDept.EMPLOYEE_SALARY);
        final JTextField txtCommission = createTextField(EmpDept.EMPLOYEE_COMMISSION);
        final DateInputPanel pnlHiredate = createDateInputPanel(EmpDept.EMPLOYEE_HIREDATE);

        setDefaultFocusComponent(txtName);

        txtName.setColumns(8);
        txtJob.setColumns(8);
        boxManager.setPreferredSize(UiUtil.getPreferredTextFieldSize());

        setLayout(new FlexibleGridLayout(3,3,5,5,true,false));

        add(createPropertyPanel(EmpDept.EMPLOYEE_NAME, txtName));
        add(createPropertyPanel(EmpDept.EMPLOYEE_JOB, txtJob));
        add(createPropertyPanel(EmpDept.EMPLOYEE_DEPARTMENT_FK, boxDepartment));

        add(createPropertyPanel(EmpDept.EMPLOYEE_MGR_FK, boxManager));
        add(createPropertyPanel(EmpDept.EMPLOYEE_SALARY, txtSalary));
        add(createPropertyPanel(EmpDept.EMPLOYEE_COMMISSION, txtCommission));

        add(createPropertyPanel(EmpDept.EMPLOYEE_HIREDATE, pnlHiredate));
        add(new JLabel());
        add(new JLabel());
      }
    };
  }
}
