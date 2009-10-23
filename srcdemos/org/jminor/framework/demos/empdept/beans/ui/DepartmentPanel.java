/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.demos.empdept.beans.EmployeeModel;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.EntityUtil;

import javax.swing.JTextField;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class DepartmentPanel extends EntityPanel {

  public DepartmentPanel(final EntityModel model) {
    super(model, EmpDept.getString(EmpDept.T_DEPARTMENT), true, false, false, EMBEDDED, true);
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
    final ControlSet controlSet = new ControlSet(Messages.get(Messages.PRINT));
    controlSet.add(ControlFactory.methodControl(this, "viewEmployeeReport", EmpDept.getString(EmpDept.EMPLOYEE_REPORT)));
    controlSet.add(getControl(PRINT));

    return controlSet;
  }

  @Override
  protected List<EntityPanelProvider> getDetailPanelProviders() {
    return Arrays.asList(new EntityPanelProvider(EmployeeModel.class, EmployeePanel.class));
  }

  @Override
  protected EntityEditPanel initializeEditPanel(final EntityEditModel editModel) {
    return new EntityEditPanel(editModel) {
      @Override
      protected void initializeUI() {
        final JTextField txtDepartmentNumber = createTextField(EmpDept.DEPARTMENT_ID, LinkType.READ_WRITE, true, null);
        final JTextField txtDepartmentName = (JTextField) UiUtil.makeUpperCase(createTextField(EmpDept.DEPARTMENT_NAME));
        final JTextField txtDepartmentLocation = (JTextField) UiUtil.makeUpperCase(createTextField(EmpDept.DEPARTMENT_LOCATION));

        setDefaultFocusComponent(txtDepartmentNumber);
        txtDepartmentNumber.setColumns(10);

        //we don't allow editing of the department number since it's a primary key
        getEditModel().stEntityNotNull.evtStateChanged.addListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            if (getEditModel().stEntityNotNull.isActive()) {
              txtDepartmentNumber.setEnabled(false);
              setDefaultFocusComponent(txtDepartmentName);
            }
            else {
              txtDepartmentNumber.setEnabled(true);
              setDefaultFocusComponent(txtDepartmentNumber);
            }
          }
        });

        setLayout(new GridLayout(3,1,5,5));
        add(createControlPanel(EmpDept.DEPARTMENT_ID, txtDepartmentNumber));
        add(createControlPanel(EmpDept.DEPARTMENT_NAME, txtDepartmentName));
        add(createControlPanel(EmpDept.DEPARTMENT_LOCATION, txtDepartmentLocation));
      }
    };
  }
}
