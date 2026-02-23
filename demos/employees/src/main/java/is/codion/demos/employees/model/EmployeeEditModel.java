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
 * Copyright (c) 2004 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.employees.model;

import is.codion.demos.employees.domain.Employees.Employee;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityEditor;
import is.codion.swing.framework.model.SwingEntityEditor.SwingComponentModels;
import is.codion.swing.framework.model.component.EntityComboBoxModel;

import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;

// tag::constructor[]
public final class EmployeeEditModel extends SwingEntityEditModel {

	public EmployeeEditModel(EntityConnectionProvider connectionProvider) {
		super(Employee.TYPE, connectionProvider, new EmployeeComponentModels());
		editor().comboBoxModels().initialize(Employee.MANAGER_FK, Employee.DEPARTMENT_FK);
		configureManagerComboBoxModel();
	}
	// end::constructor[]

	// tag::configureComboBoxModel[]
	// Configure the manager ComboBoxModel, this should not be done when creating
	// the combo box model in createComboBox(), since that method is also called each
	// time a combo box is required for editing multiple entities via the table panel
	private void configureManagerComboBoxModel() {
		//hide the employee being edited to prevent an employee from being made her own manager
		EntityComboBoxModel comboBoxModel = editor().comboBoxModels().get(Employee.MANAGER_FK);
		editor().entity().addConsumer(employee ->
						comboBoxModel.filter().predicate().set(manager ->
										!Objects.equals(manager, employee)));
		//and only show managers from the currently selected department
		editor().value(Employee.DEPARTMENT_FK).addConsumer(department ->
						comboBoxModel.filter().get(Employee.DEPARTMENT_FK)
										.set(department == null ? emptyList() : singleton(department.primaryKey())));
	}
	// end::configureComboBoxModel[]

	private static final class EmployeeComponentModels implements SwingComponentModels {

		// tag::createComboBoxModel[]
		// Providing a custom ComboBoxModel for the manager attribute, which only shows managers and the president
		@Override
		public EntityComboBoxModel comboBoxModel(ForeignKey foreignKey, SwingEntityEditor editor) {
			if (foreignKey.equals(Employee.MANAGER_FK)) {
				return EntityComboBoxModel.builder()
								.entityType(Employee.TYPE)
								.connectionProvider(editor.connectionProvider())
								//Customize the null value caption so that it displays 'None'
								//instead of the default '-' character
								.nullCaption("None")
								//Only select the president and managers from the database
								.condition(() -> Employee.JOB.in(Employee.MANAGER, Employee.PRESIDENT))
								//Prevent automatically reflecting changes to employees
								//when persisted, as we simply refresh instead, due to the
								//filter predicate, see configure() below
								.persistenceAware(false)
								.build();
			}

			return SwingComponentModels.super.comboBoxModel(foreignKey, editor);
		}
		// end::createComboBoxModel[]
	}
}