/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.ui;

import is.codion.common.Text;
import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.framework.demos.empdept.domain.EmpDept.Employee;
import is.codion.framework.demos.empdept.model.EmployeeEditModel;
import is.codion.framework.model.EntityEditModel;
import is.codion.plugin.jackson.json.domain.EntityObjectMapper;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ControlList;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityTablePanel;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

// tag::initializeEntityPanels[]
public class EmpDeptAppPanel extends EntityApplicationPanel<EmpDeptAppPanel.EmpDeptApplicationModel> {

  @Override
  protected List<EntityPanel> initializeEntityPanels(final EmpDeptApplicationModel applicationModel) {
    final SwingEntityModel departmentModel = applicationModel.getEntityModel(Department.TYPE);
    final SwingEntityModel employeeModel = departmentModel.getDetailModel(Employee.TYPE);

    final EntityPanel employeePanelBuilder = new EntityPanel(employeeModel,
            new EmployeeEditPanel(employeeModel.getEditModel()));;

    final EntityPanel departmentPanel = new EntityPanel(departmentModel,
            new DepartmentEditPanel(departmentModel.getEditModel()),
            new DepartmentTablePanel(departmentModel.getTableModel()));
    departmentPanel.addDetailPanel(employeePanelBuilder);

    departmentModel.refresh();

    return Collections.singletonList(departmentPanel);
  }
  // end::initializeEntityPanels[]

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
    toolsControls.add(Control.builder()
            .command(this::importJSON)
            .name("Import JSON")
            .build());

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
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("is.codion.framework.demos.empdept.domain.EmpDept");
    new EmpDeptAppPanel().startApplication("Emp-Dept", null, MaximizeFrame.NO,
            Windows.getScreenSizeRatio(0.6), User.parseUser("scott:tiger"));
  }
  // end::main[]

  // tag::applicationModel[]
  public static final class EmpDeptApplicationModel extends SwingEntityApplicationModel {

    public EmpDeptApplicationModel(final EntityConnectionProvider connectionProvider) {
      super(connectionProvider);
      final SwingEntityModel departmentModel = new SwingEntityModel(Department.TYPE, connectionProvider);
      departmentModel.addDetailModel(new SwingEntityModel(new EmployeeEditModel(connectionProvider)));
      addEntityModel(departmentModel);
    }
  }
  // end::applicationModel[]
}
