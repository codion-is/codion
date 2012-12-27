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
import org.jminor.framework.client.model.DefaultEntityModelProvider;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.EntityModelProvider;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.model.PropertySummaryModel;
import org.jminor.framework.client.ui.EntityApplicationPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.client.ui.EntityTablePanel;
import org.jminor.framework.db.provider.EntityConnectionProvider;
import org.jminor.framework.demos.empdept.beans.EmployeeEditModel;
import org.jminor.framework.demos.empdept.beans.ui.DepartmentEditPanel;
import org.jminor.framework.demos.empdept.beans.ui.DepartmentTablePanel;
import org.jminor.framework.demos.empdept.beans.ui.EmployeeEditPanel;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.plugins.json.EntityJSONParser;

import java.io.File;
import java.nio.charset.Charset;

import static org.jminor.framework.demos.empdept.domain.EmpDept.*;

public class EmpDeptAppPanel extends EntityApplicationPanel {
  @Override
  protected void setupEntityPanelProviders() {
    final EmployeeModelProvider employeeModelProvider = new EmployeeModelProvider();
    final EmployeePanelProvider employeePanelProvider = new EmployeePanelProvider(employeeModelProvider);
    employeePanelProvider.setEditPanelClass(EmployeeEditPanel.class);

    final EntityModelProvider departmentModelProvider = new DefaultEntityModelProvider(T_DEPARTMENT) {
      @Override
      protected void configureModel(final EntityModel entityModel) {
        entityModel.getDetailModel(EmpDept.T_EMPLOYEE).getTableModel().setQueryCriteriaRequired(false);
      }
    };
    departmentModelProvider.addDetailModelProvider(employeeModelProvider);
    final EntityPanelProvider departmentPanelProvider = new EntityPanelProvider(departmentModelProvider);
    departmentPanelProvider.setEditPanelClass(DepartmentEditPanel.class);
    departmentPanelProvider.setTablePanelClass(DepartmentTablePanel.class);
    departmentPanelProvider.addDetailPanelProvider(employeePanelProvider);

    addEntityPanelProvider(departmentPanelProvider);
  }

  public void importJSON() throws Exception {
    final File file = UiUtil.selectFile(this, null);
    UiUtil.showInDialog(this, EntityTablePanel.createStaticEntityTablePanel(new EntityJSONParser().deserialize(
            Util.getTextFileContents(file.getAbsolutePath(), Charset.defaultCharset())), getModel().getConnectionProvider()),
            true, "Import", null, null, null);
  }

  @Override
  protected ControlSet getToolsControlSet() {
    final ControlSet toolsSet = super.getToolsControlSet();
    toolsSet.add(Controls.methodControl(this, "importJSON", EmpDept.getString(IMPORT_JSON)));

    return toolsSet;
  }

  @Override
  protected EntityApplicationModel initializeApplicationModel(final EntityConnectionProvider connectionProvider) throws CancelException {
    return new EmpDeptApplicationModel(connectionProvider);
  }

  public static void main(final String[] args) {
    Configuration.setValue(Configuration.TOOLBAR_BUTTONS, true);
    Configuration.setValue(Configuration.COMPACT_ENTITY_PANEL_LAYOUT, true);
    Configuration.setValue(Configuration.USE_OPTIMISTIC_LOCKING, true);
    new EmpDeptAppPanel().startApplication("Emp-Dept", null, false, UiUtil.getScreenSizeRatio(0.6), new User("scott", "tiger"));
  }

  private static final class EmpDeptApplicationModel extends DefaultEntityApplicationModel {
    private EmpDeptApplicationModel(final EntityConnectionProvider connectionProvider) {
      super(connectionProvider);
    }

    @Override
    protected void loadDomainModel() {
      EmpDept.init();
    }
  }

  private static final class EmployeeModelProvider extends DefaultEntityModelProvider {
    private EmployeeModelProvider() {
      super(EmpDept.T_EMPLOYEE);
      setEditModelClass(EmployeeEditModel.class);
    }

    @Override
    protected void configureTableModel(final EntityTableModel tableModel) {
      tableModel.getPropertySummaryModel(EMPLOYEE_SALARY).setSummaryType(PropertySummaryModel.SummaryType.AVERAGE);
    }
  }

  private static final class EmployeePanelProvider extends EntityPanelProvider {
    private EmployeePanelProvider(final EmployeeModelProvider modelProvider) {
      super(modelProvider);
    }

    @Override
    protected void configureTablePanel(final EntityTablePanel tablePanel) {
      tablePanel.setSummaryPanelVisible(true);
    }
  }
}
