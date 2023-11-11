/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.ui;

import is.codion.common.user.User;
import is.codion.framework.demos.empdept.domain.EmpDept;
import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.framework.demos.empdept.domain.EmpDept.Employee;
import is.codion.framework.demos.empdept.model.EmpDeptAppModel;
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
import java.awt.Dimension;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import static is.codion.common.Text.textFileContents;
import static is.codion.framework.json.domain.EntityObjectMapper.entityObjectMapper;
import static is.codion.swing.framework.ui.TabbedPanelLayout.splitPaneResizeWeight;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Collections.singletonList;

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
            new DepartmentTablePanel(departmentModel.tableModel()),
            splitPaneResizeWeight(0.4));
    departmentPanel.addDetailPanel(employeePanelBuilder);

    return singletonList(departmentPanel);
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
            entityObjectMapper(applicationModel().entities()).deserializeEntities(
                    textFileContents(file.getAbsolutePath(), defaultCharset())),
            applicationModel().connectionProvider());

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
            .frameSize(new Dimension(1000, 600))
            .defaultLoginUser(User.parse("scott:tiger"))
            .start();
  }
  // end::main[]
}
