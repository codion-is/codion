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
package is.codion.demos.employees.model;

import is.codion.demos.employees.domain.Employees.Employee;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;

import java.util.Objects;

// tag::constructor[]
public final class EmployeeEditModel extends SwingEntityEditModel {

	public EmployeeEditModel(EntityConnectionProvider connectionProvider) {
		super(Employee.TYPE, connectionProvider);
		initializeComboBoxModels(Employee.MANAGER_FK, Employee.DEPARTMENT_FK);
	}
	// end::constructor[]

	// tag::createForeignKeyComboBox[]
	// Providing a custom ComboBoxModel for the manager attribute, which only shows managers and the president
	@Override
	public EntityComboBoxModel createForeignKeyComboBoxModel(ForeignKey foreignKey) {
		if (foreignKey.equals(Employee.MANAGER_FK)) {
			EntityComboBoxModel managerComboBoxModel = EntityComboBoxModel.builder(Employee.TYPE, connectionProvider())
							//Customize the null value caption so that it displays 'None'
							//instead of the default '-' character
							.nullCaption("None")
							//Only select the president and managers from the database
							.condition(() -> Employee.JOB.in(Employee.MANAGER, Employee.PRESIDENT))
							.build();
			//Refresh the manager ComboBoxModel when an employee is added, deleted or updated,
			//in case a new manager got hired, fired or promoted
			afterInsertUpdateOrDelete().addListener(managerComboBoxModel::refresh);
			//hide the employee being edited to prevent an employee from being made her own manager
			editor().addConsumer(employee ->
							managerComboBoxModel.filter().predicate().set(manager ->
											!Objects.equals(manager, employee)));
			//and only show managers from the currently selected department
			value(Employee.DEPARTMENT_FK).addConsumer(department ->
							managerComboBoxModel.filter()
											.get(Employee.DEPARTMENT_FK).set(department.primaryKey()));

			return managerComboBoxModel;
		}

		return super.createForeignKeyComboBoxModel(foreignKey);
	}
	// end::createForeignKeyComboBox[]
}