/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityTablePanel;

import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

// tag::initializeEntityPanels[]
public class EmpDeptAppPanel extends EntityApplicationPanel<EmpDeptAppPanel.EmpDeptApplicationModel> {

  public EmpDeptAppPanel() {
    super("Emp-Dept");
  }

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
    final File file = Dialogs.fileSelectionDialog()
            .owner(this)
            .addFileFilter(new FileNameExtensionFilter("JSON files", "json"))
            .addFileFilter(new FileNameExtensionFilter("Text files", "txt"))
            .selectFile();

    final EntityTablePanel tablePanel = EntityTablePanel.createReadOnlyEntityTablePanel(
            EntityObjectMapper.createEntityObjectMapper(getModel().getEntities()).deserializeEntities(
                    Text.getTextFileContents(file.getAbsolutePath(), Charset.defaultCharset())), getModel().getConnectionProvider());

    Dialogs.componentDialog(tablePanel)
            .owner(this)
            .title("Import")
            .show();
  }
  // end::importJSON[]

  // tag::getToolsControls[]
  @Override
  protected Controls getToolsControls() {
    return super.getToolsControls()
            .add(Control.builder(this::importJSON)
                    .caption("Import JSON")
                    .build());
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
    SwingUtilities.invokeLater(() -> new EmpDeptAppPanel().starter()
            .frameSize(Windows.getScreenSizeRatio(0.6))
            .defaultLoginUser(User.parseUser("scott:tiger"))
            .start());
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
