/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.ui;

import is.codion.common.Text;
import is.codion.common.model.CancelException;
import is.codion.common.model.table.ColumnSummary;
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.framework.demos.empdept.domain.EmpDept.Employee;
import is.codion.framework.demos.empdept.model.EmployeeEditModel;
import is.codion.framework.model.EntityEditModel;
import is.codion.plugin.jackson.json.domain.EntityObjectMapper;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.control.ControlList;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityModelBuilder;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityPanelBuilder;
import is.codion.swing.framework.ui.EntityTablePanel;

import java.io.File;
import java.nio.charset.Charset;

// tag::setupEntityPanelBuilders[]
public class EmpDeptAppPanel extends EntityApplicationPanel<EmpDeptAppPanel.EmpDeptApplicationModel> {

  @Override
  protected void setupEntityPanelBuilders() {
    final EmployeeModelBuilder employeeModelBuilder = new EmployeeModelBuilder();
    final EmployeePanelBuilder employeePanelBuilder = new EmployeePanelBuilder(employeeModelBuilder);

    final SwingEntityModelBuilder departmentModelBuilder = new SwingEntityModelBuilder(Department.TYPE) {
      @Override
      protected void configureModel(final SwingEntityModel entityModel) {
        entityModel.getDetailModel(Employee.TYPE).getTableModel().getQueryConditionRequiredState().set(false);
      }
    };
    //This relies on the foreign key association between employee and department
    departmentModelBuilder.detailModelBuilder(employeeModelBuilder);

    final EntityPanelBuilder departmentPanelBuilder = new EntityPanelBuilder(departmentModelBuilder)
            .editPanelClass(DepartmentEditPanel.class)
            .tablePanelClass(DepartmentTablePanel.class)
            .detailPanelBuilder(employeePanelBuilder);

    addEntityPanelBuilder(departmentPanelBuilder);
  }
  // end::setupEntityPanelBuilders[]

  // tag::importJSON[]
  public void importJSON() throws Exception {
    final File file = Dialogs.selectFile(this, null);
    Dialogs.displayInDialog(this, EntityTablePanel.createReadOnlyEntityTablePanel(
            new EntityObjectMapper(getModel().getEntities()).deserializeEntities(
                    Text.getTextFileContents(file.getAbsolutePath(), Charset.defaultCharset())), getModel().getConnectionProvider()), "Import");
  }
  // end::importJSON[]

  // tag::getToolsControls[]
  @Override
  protected ControlList getToolsControls() {
    final ControlList toolsControls = super.getToolsControls();
    toolsControls.add(Controls.control(this::importJSON, "Import JSON"));

    return toolsControls;
  }
  // end::getToolsControls[]

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
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("is.codion.framework.demos.empdept.domain.EmpDept");
    new EmpDeptAppPanel().startApplication("Emp-Dept", null, MaximizeFrame.NO,
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

  // tag::employeeModelBuilder[]
  private static final class EmployeeModelBuilder extends SwingEntityModelBuilder {
    private EmployeeModelBuilder() {
      super(Employee.TYPE);
      editModelClass(EmployeeEditModel.class);
    }

    @Override
    protected void configureTableModel(final SwingEntityTableModel tableModel) {
      tableModel.getColumnSummaryModel(Employee.SALARY).setSummary(ColumnSummary.AVERAGE);
    }
  }
  // end::employeeModelBuilder[]

  // tag::employeePanelBuilder[]
  private static final class EmployeePanelBuilder extends EntityPanelBuilder {
    private EmployeePanelBuilder(final EmployeeModelBuilder modelProvider) {
      super(modelProvider);
      editPanelClass(EmployeeEditPanel.class);
    }

    @Override
    protected void configureTablePanel(final EntityTablePanel tablePanel) {
      tablePanel.setSummaryPanelVisible(true);
    }
  }
}
// end::employeePanelBuilder[]