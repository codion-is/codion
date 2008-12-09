/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans.ui;

import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.demos.empdept.beans.EmployeeModel;
import org.jminor.framework.demos.empdept.model.EmpDept;

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.Arrays;
import java.util.List;

public class DepartmentPanel extends EntityPanel {

  public DepartmentPanel() {
    super(true, false, false, EMBEDDED, true, true);
  }

  /** {@inheritDoc} */
  protected List<EntityPanelInfo> getDetailPanelInfo() {
    return Arrays.asList(new EntityPanelInfo(EmployeeModel.class, EmployeePanel.class));
  }

  /** {@inheritDoc} */
  protected JPanel initializePropertyPanel() {
    final JTextField txtDeptno = createTextField(EmpDept.DEPARTMENT_ID, LinkType.READ_WRITE,
            true, null, getModel().stEntityActive.getReversedState());
    setDefaultFocusComponent(txtDeptno);
    txtDeptno.setColumns(10);

    final JTextField txtName = UiUtil.makeUpperCase(createTextField(EmpDept.DEPARTMENT_NAME));
    txtName.setColumns(10);

    final JPanel ret = new JPanel(new FlexibleGridLayout(3,1,5,5,true,false));
    ret.add(getControlPanel(EmpDept.DEPARTMENT_ID, txtDeptno));
    ret.add(getControlPanel(EmpDept.DEPARTMENT_NAME, txtName));
    ret.add(getControlPanel(EmpDept.DEPARTMENT_LOCATION, UiUtil.makeUpperCase(createTextField(EmpDept.DEPARTMENT_LOCATION))));

    return ret;
  }
}
