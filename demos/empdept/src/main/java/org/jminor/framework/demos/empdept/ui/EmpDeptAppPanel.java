/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.ui;

import org.jminor.common.Text;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.table.ColumnSummary;
import org.jminor.common.user.Users;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.demos.empdept.model.EmployeeEditModel;
import org.jminor.framework.model.EntityEditModel;
import org.jminor.plugin.json.EntityJSONParser;
import org.jminor.swing.common.ui.Windows;
import org.jminor.swing.common.ui.control.ControlSet;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.dialog.Dialogs;
import org.jminor.swing.framework.model.SwingEntityApplicationModel;
import org.jminor.swing.framework.model.SwingEntityModel;
import org.jminor.swing.framework.model.SwingEntityModelProvider;
import org.jminor.swing.framework.model.SwingEntityTableModel;
import org.jminor.swing.framework.ui.EntityApplicationPanel;
import org.jminor.swing.framework.ui.EntityPanel;
import org.jminor.swing.framework.ui.EntityPanelBuilder;
import org.jminor.swing.framework.ui.EntityTablePanel;

import java.io.File;
import java.nio.charset.Charset;

// tag::setupEntityPanelProviders[]
public class EmpDeptAppPanel extends EntityApplicationPanel<EmpDeptAppPanel.EmpDeptApplicationModel> {

  @Override
  protected void setupEntityPanelBuilders() {
    final EmployeeModelProvider employeeModelProvider = new EmployeeModelProvider();
    final EmployeePanelBuilder employeePanelProvider =
            new EmployeePanelBuilder(employeeModelProvider);
    employeePanelProvider.setEditPanelClass(EmployeeEditPanel.class);

    final SwingEntityModelProvider departmentModelProvider = new SwingEntityModelProvider(EmpDept.T_DEPARTMENT) {
      @Override
      protected void configureModel(final SwingEntityModel entityModel) {
        entityModel.getDetailModel(EmpDept.T_EMPLOYEE).getTableModel().getQueryConditionRequiredState().set(false);
      }
    };
    //This relies on the foreign key association between employee and department
    departmentModelProvider.addDetailModelProvider(employeeModelProvider);

    final EntityPanelBuilder departmentPanelProvider =
            new EntityPanelBuilder(departmentModelProvider);
    departmentPanelProvider.setEditPanelClass(DepartmentEditPanel.class);
    departmentPanelProvider.setTablePanelClass(DepartmentTablePanel.class);
    departmentPanelProvider.addDetailPanelBuilder(employeePanelProvider);

    addEntityPanelBuilder(departmentPanelProvider);
  }
// end::setupEntityPanelProviders[]

// tag::importJSON[]
  public void importJSON() throws Exception {
    final File file = Dialogs.selectFile(this, null);
    Dialogs.displayInDialog(this, EntityTablePanel.createReadOnlyEntityTablePanel(
            new EntityJSONParser(getModel().getDomain()).deserializeEntities(
                    Text.getTextFileContents(file.getAbsolutePath(), Charset.defaultCharset())), getModel().getConnectionProvider()), "Import");
  }
// end::importJSON[]

// tag::getToolsControlSet[]
  @Override
  protected ControlSet getToolsControlSet() {
    final ControlSet toolsSet = super.getToolsControlSet();
    toolsSet.add(Controls.control(this::importJSON, "Import JSON"));

    return toolsSet;
  }
// end::getToolsControlSet[]

// tag::initializeApplicationModel[]
  @Override
  protected EmpDeptApplicationModel initializeApplicationModel(final EntityConnectionProvider connectionProvider) throws CancelException {
    return new EmpDeptApplicationModel(connectionProvider);
  }
// end::initializeApplicationModel[]

// tag::main[]
  public static void main(final String[] args) {
    EntityEditModel.POST_EDIT_EVENTS.set(true);
    EntityPanel.TOOLBAR_BUTTONS.set(true);
    EntityPanel.COMPACT_ENTITY_PANEL_LAYOUT.set(true);
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("org.jminor.framework.demos.empdept.domain.EmpDept");
    new EmpDeptAppPanel().startApplication("Emp-Dept", null, false,
            Windows.getScreenSizeRatio(0.6), Users.parseUser("scott:tiger"));
  }
// end::main[]

// tag::applicationModel[]
  public static final class EmpDeptApplicationModel extends SwingEntityApplicationModel {

    public EmpDeptApplicationModel(final EntityConnectionProvider connectionProvider) {
      super(connectionProvider);
    }
  }
// end::applicationModel[]

// tag::employeeModelProvider[]
  private static final class EmployeeModelProvider extends SwingEntityModelProvider {
    private EmployeeModelProvider() {
      super(EmpDept.T_EMPLOYEE);
      setEditModelClass(EmployeeEditModel.class);
    }

    @Override
    protected void configureTableModel(final SwingEntityTableModel tableModel) {
      tableModel.getColumnSummaryModel(EmpDept.EMPLOYEE_SALARY).setSummary(ColumnSummary.AVERAGE);
    }
  }
// end::employeeModelProvider[]

// tag::employeePanelProvider[]
  private static final class EmployeePanelBuilder extends EntityPanelBuilder {
    private EmployeePanelBuilder(final EmployeeModelProvider modelProvider) {
      super(modelProvider);
    }

    @Override
    protected void configureTablePanel(final EntityTablePanel tablePanel) {
      tablePanel.setSummaryPanelVisible(true);
    }
  }
}
// end::employeePanelProvider[]