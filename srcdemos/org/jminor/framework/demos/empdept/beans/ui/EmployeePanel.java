/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans.ui;

import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.combobox.SteppedComboBox;
import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.demos.empdept.domain.EmpDept;

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
        final JTextField txtName = UiUtil.makeUpperCase(createTextField(EmpDept.EMPLOYEE_NAME));
        setDefaultFocusComponent(txtName);
        txtName.setColumns(8);

        final JTextField txtJob = UiUtil.makeUpperCase(createTextField(EmpDept.EMPLOYEE_JOB));
        txtJob.setColumns(8);

        final SteppedComboBox boxMgr = createEntityComboBox(EmpDept.EMPLOYEE_MGR_FK);
        boxMgr.setPreferredSize(UiUtil.getPreferredTextFieldSize());
        boxMgr.setPopupWidth(200);

        setLayout(new FlexibleGridLayout(3,3,5,5,true,false));
        add(createControlPanel(EmpDept.EMPLOYEE_NAME, txtName));
        add(createControlPanel(EmpDept.EMPLOYEE_JOB, txtJob));
        add(createControlPanel(EmpDept.EMPLOYEE_DEPARTMENT_FK, createEntityComboBox(EmpDept.EMPLOYEE_DEPARTMENT_FK)));

        add(createControlPanel(EmpDept.EMPLOYEE_MGR_FK, boxMgr));
        add(createControlPanel(EmpDept.EMPLOYEE_SALARY, createTextField(EmpDept.EMPLOYEE_SALARY)));
        add(createControlPanel(EmpDept.EMPLOYEE_COMMISSION, createTextField(EmpDept.EMPLOYEE_COMMISSION)));

        add(createControlPanel(EmpDept.EMPLOYEE_HIREDATE, createDateInputPanel(EmpDept.EMPLOYEE_HIREDATE)));
        add(new JLabel());
        add(new JLabel());
      }
    };
  }
}
