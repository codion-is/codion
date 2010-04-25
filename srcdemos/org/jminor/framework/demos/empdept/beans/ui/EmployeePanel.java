/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans.ui;

import org.jminor.common.model.valuemap.ChangeValueMapEditModel;
import org.jminor.common.ui.DateInputPanel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import static org.jminor.framework.demos.empdept.domain.EmpDept.*;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class EmployeePanel extends EntityPanel {

  public EmployeePanel(final EntityModel model) {
    super(model, EmpDept.getString(EmpDept.EMPLOYEE));
  }

  @Override
  protected void initialize() {
    getTablePanel().setSummaryPanelVisible(true);
  }

  @Override
  protected EntityEditPanel initializeEditPanel(final ChangeValueMapEditModel editModel) {
    return new EntityEditPanel((EntityEditModel) editModel) {
      @Override
      protected void initializeUI() {
        final JTextField txtName = (JTextField) UiUtil.makeUpperCase(createTextField(EMPLOYEE_NAME));
        final JTextField txtJob = (JTextField) UiUtil.makeUpperCase(createTextField(EMPLOYEE_JOB));
        final JComboBox boxManager = createEntityComboBox(EMPLOYEE_MGR_FK);
        final JComboBox boxDepartment = createEntityComboBox(EMPLOYEE_DEPARTMENT_FK);
        final JTextField txtSalary = createTextField(EMPLOYEE_SALARY);
        final JTextField txtCommission = createTextField(EMPLOYEE_COMMISSION);
        final DateInputPanel pnlHiredate = createDateInputPanel(EMPLOYEE_HIREDATE);

        setDefaultFocusComponent(txtName);

        txtName.setColumns(8);
        txtJob.setColumns(8);
        boxManager.setPreferredSize(UiUtil.getPreferredTextFieldSize());

        setLayout(new FlexibleGridLayout(3,3,5,5,true,false));

        add(createPropertyPanel(EMPLOYEE_NAME, txtName));
        add(createPropertyPanel(EMPLOYEE_JOB, txtJob));
        add(createPropertyPanel(EMPLOYEE_DEPARTMENT_FK, boxDepartment));

        add(createPropertyPanel(EMPLOYEE_MGR_FK, boxManager));
        add(createPropertyPanel(EMPLOYEE_SALARY, txtSalary));
        add(createPropertyPanel(EMPLOYEE_COMMISSION, txtCommission));

        add(createPropertyPanel(EMPLOYEE_HIREDATE, pnlHiredate));
        add(new JLabel());
        add(new JLabel());
      }
    };
  }
}
