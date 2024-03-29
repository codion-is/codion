/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
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
import is.codion.swing.framework.ui.TabbedDetailLayout;

import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import static is.codion.common.Text.textFileContents;
import static is.codion.framework.json.domain.EntityObjectMapper.entityObjectMapper;
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
						config -> config.detailLayout(TabbedDetailLayout.builder()
										.splitPaneResizeWeight(0.4)
										.build()));
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
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel,
						config -> config.includePopupMenu(false));

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
		Arrays.stream(FlatAllIJThemes.INFOS)
						.forEach(LookAndFeelProvider::addLookAndFeelProvider);
		EntityPanel.Config.TOOLBAR_CONTROLS.set(true);
		EntityApplicationPanel.builder(EmployeesAppModel.class, EmployeesAppPanel.class)
						.applicationName("Employees")
						.domainType(Employees.DOMAIN)
						.defaultLookAndFeelClassName(DEFAULT_FLAT_LOOK_AND_FEEL)
						.defaultLoginUser(User.parse("scott:tiger"))
						.start();
	}
	// end::main[]
}
