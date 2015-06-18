/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans.ui;

import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.ui.EntityEditPanel;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import static org.jminor.framework.demos.empdept.domain.EmpDept.*;

public class EmployeeEditPanel extends EntityEditPanel {

  public EmployeeEditPanel(final EntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    final JTextField txtName = (JTextField) UiUtil.makeUpperCase(createTextField(EMPLOYEE_NAME));
    final JTextField txtJob = (JTextField) UiUtil.makeUpperCase(createTextField(EMPLOYEE_JOB));
    final JComboBox boxManager = createEntityComboBox(EMPLOYEE_MGR_FK);
    createEntityComboBox(EMPLOYEE_DEPARTMENT_FK);
    createTextField(EMPLOYEE_SALARY);
    createTextField(EMPLOYEE_COMMISSION);
    createDateInputPanel(EMPLOYEE_HIREDATE, true);

    setInitialFocusComponent(txtName);

    txtName.setColumns(8);
    txtJob.setColumns(8);
    boxManager.setPreferredSize(UiUtil.getPreferredTextFieldSize());

    setLayout(new FlexibleGridLayout(3,3,5,5,true,false));

    addPropertyPanel(EMPLOYEE_NAME);
    addPropertyPanel(EMPLOYEE_JOB);
    addPropertyPanel(EMPLOYEE_DEPARTMENT_FK);

    addPropertyPanel(EMPLOYEE_MGR_FK);
    addPropertyPanel(EMPLOYEE_SALARY);
    addPropertyPanel(EMPLOYEE_COMMISSION);

    addPropertyPanel(EMPLOYEE_HIREDATE);
    add(new JLabel());
    add(new JLabel());
  }
}
