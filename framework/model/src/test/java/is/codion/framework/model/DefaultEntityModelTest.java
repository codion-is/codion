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
 * Copyright (c) 2018 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.model.EntityEditor.ComponentModels;
import is.codion.framework.model.test.AbstractEntityModelTest;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;

public class DefaultEntityModelTest extends AbstractEntityModelTest<DefaultEntityModelTest.TestEntityModel,
				DefaultEntityModelTest.TestEntityEditModel, DefaultEntityModelTest.TestEntityTableModel, DefaultEntityModelTest.TestEntityEditor> {

	@Override
	protected TestEntityModel createDepartmentModel() {
		TestEntityModel deptModel = new TestEntityModel(new TestEntityEditModel(Department.TYPE, connection()));
		TestEntityModel empModel = new TestEntityModel(new TestEntityEditModel(Employee.TYPE, connection()));
		deptModel.detail().add(ForeignKeyModelLink.builder()
						.model(empModel)
						.foreignKey(Employee.DEPARTMENT_FK)
						.active(true)
						.build());

		return deptModel;
	}

	@Override
	protected TestEntityModel createDepartmentModelWithoutDetailModel() {
		return new TestEntityModel(new TestEntityEditModel(Department.TYPE, connection()));
	}

	@Override
	protected TestEntityModel createEmployeeModel() {
		return new TestEntityModel(new TestEntityEditModel(Employee.TYPE, connection()));
	}

	public static final class TestEntityEditModel extends AbstractEntityEditModel<TestEntityEditor> {

		public TestEntityEditModel(EntityType entityType, EntityConnection connection) {
			super(new TestEntityEditor(entityType, connection));
		}
	}

	public static final class TestEntityModel extends AbstractEntityModel<TestEntityModel, TestEntityEditModel, TestEntityTableModel, TestEntityEditor> {
		public TestEntityModel(TestEntityEditModel editModel) {
			super(editModel);
		}
	}

	public static final class TestEntityEditor extends AbstractEntityEditor<TestEntityEditor> {

		public TestEntityEditor(EntityType entityType, EntityConnection connection) {
			this(entityType, connection, new TestComponentModels() {});
		}

		public TestEntityEditor(EntityType entityType, EntityConnection connection, TestComponentModels componentModels) {
			super(entityType, connection, componentModels);
		}

		@Override
		public TestEntityEditor create(EntityType entityType) {
			return new TestEntityEditor(entityType, connection());
		}

		@Override
		public TestEntityEditor create(EntityType entityType, ComponentModels componentModels) {
			return new TestEntityEditor(entityType, connection(), (TestComponentModels) componentModels);
		}
	}

	public interface TestComponentModels extends ComponentModels {}

	public interface TestEntityTableModel extends EntityTableModel<TestEntityEditModel, TestEntityEditor> {}
}
