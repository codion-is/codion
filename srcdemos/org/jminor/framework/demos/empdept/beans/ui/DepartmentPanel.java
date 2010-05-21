/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.demos.empdept.beans.EmployeeModel;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import static org.jminor.framework.demos.empdept.domain.EmpDept.*;
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
    super(model, EmpDept.getString(EmpDept.DEPARTMENT));
  }

  public void viewEmployeeReport() throws Exception {
    if (getModel().getTableModel().getSelectionModel().isSelectionEmpty())
      return;

    final String reportPath = System.getProperty(Configuration.REPORT_PATH) + "/empdept_employees.jasper";
    final Collection<Object> departmentNumbers =
            EntityUtil.getDistinctPropertyValues(getModel().getTableModel().getSelectedItems(), EmpDept.DEPARTMENT_ID);
    final HashMap<String, Object> reportParameters = new HashMap<String, Object>();
    reportParameters.put("DEPTNO", departmentNumbers);
    viewJdbcReport(reportPath, reportParameters, null);
  }

  @Override
  public ControlSet getPrintControls() {
    final ControlSet controlSet = new ControlSet(Messages.get(Messages.PRINT));
    controlSet.add(ControlFactory.methodControl(this, "viewEmployeeReport", EmpDept.getString(EmpDept.EMPLOYEE_REPORT)));
    controlSet.add(getTablePanel().getPrintControl());

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
        final JTextField txtDepartmentNumber = createTextField(DEPARTMENT_ID);
        final JTextField txtDepartmentName = (JTextField) UiUtil.makeUpperCase(createTextField(DEPARTMENT_NAME));
        final JTextField txtDepartmentLocation = (JTextField) UiUtil.makeUpperCase(createTextField(DEPARTMENT_LOCATION));

        setInitialFocusComponent(txtDepartmentNumber);
        txtDepartmentNumber.setColumns(10);

        //we don't allow editing of the department number since it's a primary key
        getEditModel().getEntityNullState().eventStateChanged().addListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            if (getEditModel().isEntityNull()) {
              txtDepartmentNumber.setEnabled(true);
              setInitialFocusComponent(txtDepartmentNumber);
            }
            else {
              txtDepartmentNumber.setEnabled(false);
              setInitialFocusComponent(txtDepartmentName);
            }
          }
        });

        setLayout(new GridLayout(3,1,5,5));
        add(createPropertyPanel(DEPARTMENT_ID, txtDepartmentNumber));
        add(createPropertyPanel(DEPARTMENT_NAME, txtDepartmentName));
        add(createPropertyPanel(DEPARTMENT_LOCATION, txtDepartmentLocation));
      }
    };
  }
}
