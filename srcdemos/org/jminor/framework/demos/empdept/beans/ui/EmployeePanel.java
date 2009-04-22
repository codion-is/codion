/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans.ui;

import org.jminor.common.model.formats.ShortDotDateFormat;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.combobox.SteppedComboBox;
import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.PropertySummaryPanel;
import org.jminor.framework.demos.empdept.model.EmpDept;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class EmployeePanel extends EntityPanel {

  /** {@inheritDoc} */
  public void initialize() {
    if (isInitialized())
      return;

    super.initialize();
    getTablePanel().getSummaryProvider(EmpDept.EMPLOYEE_SALARY).setSummaryType(PropertySummaryPanel.SummaryType.AVARAGE);
    getTablePanel().setSummaryPanelVisible(true);
  }

  /** {@inheritDoc} */
  protected JPanel initializePropertyPanel() {
    final JTextField txtName = UiUtil.makeUpperCase(createTextField(EmpDept.EMPLOYEE_NAME));
    setDefaultFocusComponent(txtName);
    txtName.setColumns(8);

    final JTextField txtJob = UiUtil.makeUpperCase(createTextField(EmpDept.EMPLOYEE_JOB));
    txtJob.setColumns(8);

    final SteppedComboBox boxMgr = createEntityComboBox(EmpDept.EMPLOYEE_MGR_REF);
    boxMgr.setPreferredSize(UiUtil.getPreferredTextFieldSize());
    boxMgr.setPopupWidth(200);

    final JPanel ret = new JPanel(new FlexibleGridLayout(3,3,5,5,true,false));
    ret.add(createControlPanel(EmpDept.EMPLOYEE_NAME, txtName));
    ret.add(createControlPanel(EmpDept.EMPLOYEE_JOB, txtJob));
    ret.add(createControlPanel(EmpDept.EMPLOYEE_DEPARTMENT_REF, createEntityComboBox(EmpDept.EMPLOYEE_DEPARTMENT_REF)));

    ret.add(createControlPanel(EmpDept.EMPLOYEE_MGR_REF, boxMgr));
    ret.add(createControlPanel(EmpDept.EMPLOYEE_SALARY, createTextField(EmpDept.EMPLOYEE_SALARY)));
    ret.add(createControlPanel(EmpDept.EMPLOYEE_COMMISSION, createTextField(EmpDept.EMPLOYEE_COMMISSION)));

    ret.add(createControlPanel(EmpDept.EMPLOYEE_HIREDATE, createDateInputPanel(EmpDept.EMPLOYEE_HIREDATE, new ShortDotDateFormat())));
    ret.add(new JLabel());
    ret.add(new JLabel());

    return ret;
  }
}
