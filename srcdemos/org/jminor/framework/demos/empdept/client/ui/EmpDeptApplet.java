/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.client.ui;

import org.jminor.framework.client.model.DefaultEntityModelProvider;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.model.PropertySummaryModel;
import org.jminor.framework.client.ui.EntityApplet;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.client.ui.EntityTablePanel;
import org.jminor.framework.demos.empdept.beans.EmployeeEditModel;
import org.jminor.framework.demos.empdept.beans.ui.DepartmentEditPanel;
import org.jminor.framework.demos.empdept.beans.ui.DepartmentTablePanel;
import org.jminor.framework.demos.empdept.beans.ui.EmployeeEditPanel;
import org.jminor.framework.demos.empdept.domain.EmpDept;

/**
 * User: Björn Darri
 * Date: 3.7.2010
 * Time: 23:02:08
 */
public class EmpDeptApplet extends EntityApplet {

  public EmpDeptApplet() {
    super(new EntityPanelProvider(new DefaultEntityModelProvider(EmpDept.T_DEPARTMENT) {
      @Override
      protected void configureTableModel(final EntityTableModel tableModel) {
        tableModel.setQueryCriteriaRequired(true);
        tableModel.getPropertySummaryModel(EmpDept.EMPLOYEE_SALARY).setSummaryType(PropertySummaryModel.SummaryType.AVERAGE);
      }
    }.setEditModelClass(EmployeeEditModel.class)).setEditPanelClass(DepartmentEditPanel.class)
            .setTablePanelClass(DepartmentTablePanel.class).addDetailPanelProvider(new EntityPanelProvider(EmpDept.T_EMPLOYEE) {

      @Override
      protected void configureTablePanel(final EntityTablePanel tablePanel) {
        tablePanel.setSummaryPanelVisible(true);
      }
    }.setEditPanelClass(EmployeeEditPanel.class)));
  }
}
