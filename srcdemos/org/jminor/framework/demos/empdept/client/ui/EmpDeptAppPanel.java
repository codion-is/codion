/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.client.ui;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.control.Controls;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.DefaultEntityApplicationModel;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.model.PropertySummaryModel;
import org.jminor.framework.client.ui.EntityApplicationPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.client.ui.EntityTablePanel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.empdept.beans.EmployeeEditModel;
import org.jminor.framework.demos.empdept.beans.ui.DepartmentEditPanel;
import org.jminor.framework.demos.empdept.beans.ui.DepartmentTablePanel;
import org.jminor.framework.demos.empdept.beans.ui.EmployeeEditPanel;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import static org.jminor.framework.demos.empdept.domain.EmpDept.*;
import org.jminor.framework.plugins.json.EntityJSONParser;

import java.io.File;
import java.nio.charset.Charset;

public class EmpDeptAppPanel extends EntityApplicationPanel {

  public EmpDeptAppPanel() {
    final EntityPanelProvider employeePanelProvider = new EmployeePanelProvider();
    employeePanelProvider.setEditModelClass(EmployeeEditModel.class);
    employeePanelProvider.setEditPanelClass(EmployeeEditPanel.class);

    final EntityPanelProvider departmentPanelProvider = new EntityPanelProvider(T_DEPARTMENT);
    departmentPanelProvider.setEditPanelClass(DepartmentEditPanel.class);
    departmentPanelProvider.setTablePanelClass(DepartmentTablePanel.class);
    departmentPanelProvider.addDetailPanelProvider(employeePanelProvider);

    addMainApplicationPanelProvider(departmentPanelProvider);
  }

  public void importJSON() throws Exception {
    final File file = UiUtil.selectFile(this, null);
    UiUtil.showInDialog(this, EntityTablePanel.createStaticEntityTablePanel(new EntityJSONParser().deserialize(
            Util.getTextFileContents(file.getAbsolutePath(), Charset.defaultCharset())), getModel().getDbProvider()),
            true, "Import", null, null, null);
  }

  @Override
  protected ControlSet getToolsControlSet() {
    final ControlSet toolsSet = super.getToolsControlSet();
    toolsSet.add(Controls.methodControl(this, "importJSON", EmpDept.getString(IMPORT_JSON)));

    return toolsSet;
  }

  @Override
  protected void configureApplication() {
    Configuration.setValue(Configuration.TOOLBAR_BUTTONS, true);
    Configuration.setValue(Configuration.COMPACT_ENTITY_PANEL_LAYOUT, true);
    Configuration.setValue(Configuration.USE_OPTIMISTIC_LOCKING, true);
  }

  @Override
  protected EntityApplicationModel initializeApplicationModel(final EntityDbProvider dbProvider) throws CancelException {
    return new EmpDeptApplicationModel(dbProvider);
  }

  public static void main(final String[] args) {
    new EmpDeptAppPanel().startApplication("Emp-Dept", null, false, UiUtil.getScreenSizeRatio(0.6), new User("scott", "tiger"));
  }

  private static final class EmpDeptApplicationModel extends DefaultEntityApplicationModel {
    public EmpDeptApplicationModel(final EntityDbProvider dbProvider) {
      super(dbProvider);
    }

    @Override
    protected void loadDomainModel() {
      new EmpDept();
    }
  }

  private static final class EmployeePanelProvider extends EntityPanelProvider {
    public EmployeePanelProvider() {
      super(EmpDept.T_EMPLOYEE);
    }

    @Override
    protected void configureTableModel(final EntityTableModel tableModel) {
      tableModel.setQueryCriteriaRequired(false);
      tableModel.getPropertySummaryModel(EMPLOYEE_SALARY).setSummaryType(PropertySummaryModel.SummaryType.AVERAGE);
    }

    @Override
    protected void configureTablePanel(final EntityTablePanel tablePanel) {
      tablePanel.setSummaryPanelVisible(true);
    }
  }
}
