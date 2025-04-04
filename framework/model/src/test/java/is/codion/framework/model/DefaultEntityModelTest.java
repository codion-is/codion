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

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.model.test.AbstractEntityModelTest;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;

public class DefaultEntityModelTest extends AbstractEntityModelTest<DefaultEntityModelTest.TestEntityModel,
				DefaultEntityModelTest.TestEntityEditModel, DefaultEntityModelTest.TestEntityTableModel> {

	@Override
	protected TestEntityModel createDepartmentModel() {
		TestEntityModel deptModel = new TestEntityModel(new TestEntityEditModel(Department.TYPE, connectionProvider()));
		TestEntityModel empModel = new TestEntityModel(new TestEntityEditModel(Employee.TYPE, connectionProvider()));
		deptModel.detailModels().add(deptModel.link(empModel)
						.active(true)
						.build());

		return deptModel;
	}

	@Override
	protected TestEntityModel createDepartmentModelWithoutDetailModel() {
		return new TestEntityModel(new TestEntityEditModel(Department.TYPE, connectionProvider()));
	}

	@Override
	protected TestEntityModel createEmployeeModel() {
		return new TestEntityModel(new TestEntityEditModel(Employee.TYPE, connectionProvider()));
	}

	public static final class TestEntityEditModel extends AbstractEntityEditModel {

		public TestEntityEditModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
			super(entityType, connectionProvider);
		}
	}

	public static final class TestEntityModel extends AbstractEntityModel<TestEntityModel, TestEntityEditModel, TestEntityTableModel> {
		public TestEntityModel(TestEntityEditModel editModel) {
			super(editModel);
		}
	}

	public interface TestEntityTableModel extends EntityTableModel<TestEntityEditModel> {}
}
