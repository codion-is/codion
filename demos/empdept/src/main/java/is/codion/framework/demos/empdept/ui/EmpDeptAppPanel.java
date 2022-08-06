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

// tag::createEntityPanels[]
public class EmpDeptAppPanel extends EntityApplicationPanel<EmpDeptAppPanel.EmpDeptApplicationModel> {

  public EmpDeptAppPanel() {
    super("Emp-Dept");
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
            EntityObjectMapper.createEntityObjectMapper(model().entities()).deserializeEntities(
                    Text.getTextFileContents(file.getAbsolutePath(), Charset.defaultCharset())), model().connectionProvider());

    Dialogs.componentDialog(tablePanel)
            .owner(this)
            .title("Import")
            .show();
  }
  // end::importJSON[]

  // tag::createToolsControls[]
  @Override
  protected Controls createToolsControls() {
    return super.createToolsControls()
            .add(Control.builder(this::importJSON)
                    .caption("Import JSON")
                    .build());
  }
  // end::createToolsControls[]

  // tag::createApplicationModel[]
  @Override
  protected EmpDeptApplicationModel createApplicationModel(EntityConnectionProvider connectionProvider) throws CancelException {
    return new EmpDeptApplicationModel(connectionProvider);
  }
  // end::createApplicationModel[]

  // tag::main[]
  public static void main(String[] args) {
    EntityEditModel.POST_EDIT_EVENTS.set(true);
    EntityPanel.TOOLBAR_BUTTONS.set(true);
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("is.codion.framework.demos.empdept.domain.EmpDept");
    SwingUtilities.invokeLater(() -> new EmpDeptAppPanel().starter()
            .frameSize(Windows.getScreenSizeRatio(0.6))
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
