/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.ui;

import is.codion.common.Text;
import is.codion.common.user.User;
import is.codion.framework.demos.empdept.domain.EmpDept;
import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.framework.demos.empdept.domain.EmpDept.Employee;
import is.codion.framework.demos.empdept.model.EmpDeptAppModel;
import is.codion.framework.json.domain.EntityObjectMapper;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityTablePanel;

import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// tag::createEntityPanels[]
public class EmpDeptAppPanel extends EntityApplicationPanel<EmpDeptAppModel> {

  private static final String DEFAULT_FLAT_LOOK_AND_FEEL = "com.formdev.flatlaf.intellijthemes.FlatArcIJTheme";

  public EmpDeptAppPanel(EmpDeptAppModel applicationModel) {
    super(applicationModel);
  }

  @Override
  protected List<EntityPanel> createEntityPanels() {
    SwingEntityModel departmentModel = applicationModel().entityModel(Department.TYPE);
    SwingEntityModel employeeModel = departmentModel.detailModel(Employee.TYPE);

    EntityPanel employeePanelBuilder = new EntityPanel(employeeModel,
            new EmployeeEditPanel(employeeModel.editModel()));

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

    EntityTablePanel tablePanel = EntityTablePanel.entityTablePanelReadOnly(
            EntityObjectMapper.entityObjectMapper(applicationModel().entities()).deserializeEntities(
                    Text.textFileContents(file.getAbsolutePath(), Charset.defaultCharset())), applicationModel().connectionProvider());

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
                    .name("Import JSON")
                    .build());
  }
  // end::createToolsMenuControls[]

  // tag::main[]
  public static void main(String[] args) {
    EntityPanel.TOOLBAR_CONTROLS.set(true);
    Arrays.stream(FlatAllIJThemes.INFOS)
            .forEach(LookAndFeelProvider::addLookAndFeelProvider);
    EntityApplicationPanel.builder(EmpDeptAppModel.class, EmpDeptAppPanel.class)
            .applicationName("Emp-Dept")
            .domainType(EmpDept.DOMAIN)
            .defaultLookAndFeelClassName(DEFAULT_FLAT_LOOK_AND_FEEL)
            .frameSize(Windows.screenSizeRatio(0.6))
            .defaultLoginUser(User.parse("scott:tiger"))
            .start();
  }
  // end::main[]
}
