/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.ui;

import is.codion.common.Text;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.framework.demos.empdept.domain.EmpDept.Employee;
import is.codion.framework.demos.empdept.model.EmployeeEditModel;
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

import static is.codion.swing.framework.ui.EntityApplicationBuilder.entityApplicationBuilder;

// tag::createEntityPanels[]
public class EmpDeptAppPanel extends EntityApplicationPanel<EmpDeptAppPanel.EmpDeptApplicationModel> {

  public EmpDeptAppPanel(EmpDeptAppPanel.EmpDeptApplicationModel applicationModel) {
    super(applicationModel);
  }

  @Override
  protected List<EntityPanel> createEntityPanels(EmpDeptApplicationModel applicationModel) {
    SwingEntityModel departmentModel = applicationModel.entityModel(Department.TYPE);
    SwingEntityModel employeeModel = departmentModel.detailModel(Employee.TYPE);

    EntityPanel employeePanelBuilder = new EntityPanel(employeeModel,
            new EmployeeEditPanel(employeeModel.editModel()));;

    EntityPanel departmentPanel = new EntityPanel(departmentModel,
            new DepartmentEditPanel(departmentModel.editModel()),
            new DepartmentTablePanel(departmentModel.tableModel()));
    departmentPanel.addDetailPanel(employeePanelBuilder);

    return Collections.singletonList(departmentPanel);
  }
  // end::createEntityPanels[]

  // tag::importJSON[]
  public void importJSON() throws Exception {
    File file = Dialogs.fileSelectionDialog()
            .owner(this)
            .fileFilter(new FileNameExtensionFilter("JSON files", "json"))
            .fileFilter(new FileNameExtensionFilter("Text files", "txt"))
            .selectFile();

    EntityTablePanel tablePanel = EntityTablePanel.createReadOnlyEntityTablePanel(
            EntityObjectMapper.entityObjectMapper(model().entities()).deserializeEntities(
                    Text.textFileContents(file.getAbsolutePath(), Charset.defaultCharset())), model().connectionProvider());

    Dialogs.componentDialog(tablePanel)
            .owner(this)
            .title("Import")
            .show();
  }
  // end::importJSON[]

  // tag::createToolsMenuControls[]
  @Override
  protected Controls createToolsMenuControls() {
    return super.createToolsMenuControls()
            .add(Control.builder(this::importJSON)
                    .caption("Import JSON")
                    .build());
  }
  // end::createToolsMenuControls[]

  // tag::main[]
  public static void main(String[] args) {
    EntityPanel.TOOLBAR_BUTTONS.set(true);
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("is.codion.framework.demos.empdept.domain.EmpDept");
    SwingUtilities.invokeLater(() -> entityApplicationBuilder(EmpDeptApplicationModel.class, EmpDeptAppPanel.class)
            .applicationName("Emp-Dept")
            .frameSize(Windows.screenSizeRatio(0.6))
            .defaultLoginUser(User.parse("scott:tiger"))
            .start());
  }
  // end::main[]

  // tag::applicationModel[]
  public static final class EmpDeptApplicationModel extends SwingEntityApplicationModel {

    public EmpDeptApplicationModel(EntityConnectionProvider connectionProvider) {
      super(connectionProvider);
      SwingEntityModel departmentModel = new SwingEntityModel(Department.TYPE, connectionProvider);
      departmentModel.addDetailModel(new SwingEntityModel(new EmployeeEditModel(connectionProvider)));
      departmentModel.tableModel().refresh();
      addEntityModel(departmentModel);
    }
  }
  // end::applicationModel[]
}
