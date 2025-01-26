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
 * Copyright (c) 2018 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.framework.model.DefaultEntityModelTest.TestEntityEditModel;
import is.codion.framework.model.DefaultEntityModelTest.TestEntityModel;
import is.codion.framework.model.DefaultEntityModelTest.TestEntityTableModel;
import is.codion.framework.model.test.AbstractEntityApplicationModelTest;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;

public final class DefaultEntityApplicationModelTest extends AbstractEntityApplicationModelTest<TestEntityModel,
				TestEntityEditModel, TestEntityTableModel> {

	@Override
	protected TestEntityModel createDepartmentModel() {
		TestEntityModel deptModel = new TestEntityModel(
						new TestEntityEditModel(Department.TYPE, connectionProvider()));
		TestEntityModel empModel = new TestEntityModel(
						new TestEntityEditModel(Employee.TYPE, connectionProvider()));
		deptModel.detailModels().add(deptModel.link(empModel)
						.active(true)
						.build());

		return deptModel;
	}
}
