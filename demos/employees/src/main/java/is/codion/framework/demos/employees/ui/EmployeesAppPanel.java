/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.employees.ui;

import is.codion.common.user.User;
import is.codion.framework.demos.employees.domain.Employees;
import is.codion.framework.demos.employees.domain.Employees.Department;
import is.codion.framework.demos.employees.domain.Employees.Employee;
import is.codion.framework.demos.employees.model.EmployeesAppModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
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
public class EmployeesAppPanel extends EntityApplicationPanel<EmployeesAppModel> {

  private static final String DEFAULT_FLAT_LOOK_AND_FEEL = "com.formdev.flatlaf.intellijthemes.FlatArcIJTheme";

  public EmployeesAppPanel(EmployeesAppModel applicationModel) {
    super(applicationModel);
  }

  @Override
  protected List<EntityPanel> createEntityPanels() {
    SwingEntityModel departmentModel = applicationModel().entityModel(Department.TYPE);
    SwingEntityModel employeeModel = departmentModel.detailModel(Employee.TYPE);

    EntityPanel employeePanel = new EntityPanel(employeeModel,
            new EmployeeEditPanel(employeeModel.editModel()));

    EntityPanel departmentPanel = new EntityPanel(departmentModel,
            new DepartmentEditPanel(departmentModel.editModel()),
            new DepartmentTablePanel(departmentModel.tableModel()),
            splitPaneResizeWeight(0.4));
    departmentPanel.addDetailPanel(employeePanel);

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

    List<Entity> entities = entityObjectMapper(applicationModel().entities())
            .deserializeEntities(textFileContents(file.getAbsolutePath(), defaultCharset()));

    SwingEntityTableModel tableModel = SwingEntityTableModel.tableModel(entities, applicationModel().connectionProvider());
    tableModel.editModel().readOnly().set(true);
    EntityTablePanel tablePanel = new EntityTablePanel(tableModel);
    tablePanel.configure().includePopupMenu(false);

    Dialogs.componentDialog(tablePanel.initialize())
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
    EntityApplicationPanel.builder(EmployeesAppModel.class, EmployeesAppPanel.class)
            .applicationName("Employees")
            .domainType(Employees.DOMAIN)
            .defaultLookAndFeelClassName(DEFAULT_FLAT_LOOK_AND_FEEL)
            .frameSize(new Dimension(1000, 600))
            .defaultLoginUser(User.parse("scott:tiger"))
            .start();
  }
  // end::main[]
}
