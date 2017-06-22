/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.client.ui;

import org.jminor.common.TextUtil;
import org.jminor.common.User;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.table.ColumnSummary;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.empdept.beans.EmployeeEditModel;
import org.jminor.framework.demos.empdept.beans.ui.DepartmentEditPanel;
import org.jminor.framework.demos.empdept.beans.ui.DepartmentTablePanel;
import org.jminor.framework.demos.empdept.beans.ui.EmployeeEditPanel;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.plugins.json.EntityJSONParser;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.control.ControlSet;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.framework.model.SwingEntityApplicationModel;
import org.jminor.swing.framework.model.SwingEntityModel;
import org.jminor.swing.framework.model.SwingEntityModelProvider;
import org.jminor.swing.framework.model.SwingEntityTableModel;
import org.jminor.swing.framework.ui.EntityApplicationPanel;
import org.jminor.swing.framework.ui.EntityPanelProvider;
import org.jminor.swing.framework.ui.EntityTablePanel;

import java.io.File;
import java.nio.charset.Charset;

import static org.jminor.framework.demos.empdept.domain.EmpDept.*;

public class EmpDeptAppPanel extends EntityApplicationPanel<EmpDeptAppPanel.EmpDeptApplicationModel> {

  @Override
  protected void setupEntityPanelProviders() {
    final EmployeeModelProvider employeeModelProvider = new EmployeeModelProvider();
    final EmployeePanelProvider employeePanelProvider = new EmployeePanelProvider(employeeModelProvider);
    employeePanelProvider.setEditPanelClass(EmployeeEditPanel.class);

    final SwingEntityModelProvider departmentModelProvider = new SwingEntityModelProvider(T_DEPARTMENT) {
      @Override
      protected void configureModel(final SwingEntityModel entityModel) {
        entityModel.getDetailModel(EmpDept.T_EMPLOYEE).getTableModel().setQueryConditionRequired(false);
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
    UiUtil.displayInDialog(this, EntityTablePanel.createStaticEntityTablePanel(EntityJSONParser.deserializeEntities(
            TextUtil.getTextFileContents(file.getAbsolutePath(), Charset.defaultCharset())), getModel().getConnectionProvider()), "Import");
  }

  @Override
  protected ControlSet getToolsControlSet() {
    final ControlSet toolsSet = super.getToolsControlSet();
    toolsSet.add(Controls.commandControl(this::importJSON, EmpDept.getString(IMPORT_JSON)));

    return toolsSet;
  }

  @Override
  protected EmpDeptApplicationModel initializeApplicationModel(final EntityConnectionProvider connectionProvider) throws CancelException {
    return new EmpDeptApplicationModel(connectionProvider);
  }

  public static void main(final String[] args) {
    Configuration.setValue(Configuration.TOOLBAR_BUTTONS, true);
    Configuration.setValue(Configuration.COMPACT_ENTITY_PANEL_LAYOUT, true);
    Configuration.setValue(Configuration.USE_OPTIMISTIC_LOCKING, true);
    new EmpDeptAppPanel().startApplication("Emp-Dept", null, false, UiUtil.getScreenSizeRatio(0.6), new User("scott", "tiger"));
  }

  public static final class EmpDeptApplicationModel extends SwingEntityApplicationModel {
    public EmpDeptApplicationModel(final EntityConnectionProvider connectionProvider) {
      super(connectionProvider);
    }

    @Override
    protected void loadDomainModel() {
      EmpDept.init();
    }
  }

  private static final class EmployeeModelProvider extends SwingEntityModelProvider {
    private EmployeeModelProvider() {
      super(EmpDept.T_EMPLOYEE);
      setEditModelClass(EmployeeEditModel.class);
    }

    @Override
    protected void configureTableModel(final SwingEntityTableModel tableModel) {
      tableModel.getColumnSummaryModel(EMPLOYEE_SALARY).setSummary(ColumnSummary.AVERAGE);
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
