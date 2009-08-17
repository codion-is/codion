/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans.ui;

import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.demos.empdept.beans.EmployeeModel;
import org.jminor.framework.demos.empdept.model.EmpDept;
import org.jminor.framework.domain.EntityUtil;

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class DepartmentPanel extends EntityPanel {

  public DepartmentPanel(final EntityModel model) {
    super(model, EmpDept.getString(EmpDept.T_DEPARTMENT), true, false, false, EMBEDDED, true, true);
  }

  public void viewEmployeeReport() throws Exception {
    if (getModel().getTableModel().getSelectionModel().isSelectionEmpty())
      return;

    final String reportPath = System.getProperty(Configuration.REPORT_PATH) + "/empdept_employees.jasper";
    final Collection<Object> departmentNumbers =
            EntityUtil.getPropertyValues(getModel().getTableModel().getSelectedEntities(), EmpDept.DEPARTMENT_ID);
    final HashMap<String, Object> reportParameters = new HashMap<String, Object>();
    reportParameters.put("DEPTNO", departmentNumbers);
    viewJdbcReport(reportPath, reportParameters, null);
  }

  @Override
  public ControlSet getPrintControls() {
    final ControlSet ret = new ControlSet("Print");
    ret.add(ControlFactory.methodControl(this, "viewEmployeeReport", "Employee report"));
    ret.add(getControl(PRINT));

    return ret;
  }

  /** {@inheritDoc} */
  @Override
  protected List<EntityPanelProvider> getDetailPanelProviders() {
    return Arrays.asList(new EntityPanelProvider(EmployeeModel.class, EmployeePanel.class));
  }

  /** {@inheritDoc} */
  @Override
  protected JPanel initializePropertyPanel() {
    final JTextField txtDeptno =
            createTextField(EmpDept.DEPARTMENT_ID, LinkType.READ_WRITE, true, null);
    setDefaultFocusComponent(txtDeptno);
    txtDeptno.setColumns(10);

    final JTextField txtName = UiUtil.makeUpperCase(createTextField(EmpDept.DEPARTMENT_NAME));
    txtName.setColumns(10);

    //we don't allow editing of the department number since it's a primary key
    getModel().stEntityActive.evtStateChanged.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (getModel().stEntityActive.isActive()) {
          txtDeptno.setEnabled(false);
          setDefaultFocusComponent(txtName);
        }
        else {
          txtDeptno.setEnabled(true);
          setDefaultFocusComponent(txtDeptno);
        }
      }
    });

    final JPanel ret = new JPanel(new FlexibleGridLayout(3,1,5,5,true,false));
    ret.add(createControlPanel(EmpDept.DEPARTMENT_ID, txtDeptno));
    ret.add(createControlPanel(EmpDept.DEPARTMENT_NAME, txtName));
    ret.add(createControlPanel(EmpDept.DEPARTMENT_LOCATION, UiUtil.makeUpperCase(createTextField(EmpDept.DEPARTMENT_LOCATION))));

    return ret;
  }
}
