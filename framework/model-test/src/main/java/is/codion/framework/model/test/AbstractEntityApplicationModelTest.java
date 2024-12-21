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
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.model.test;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.model.AbstractEntityEditModel;
import is.codion.framework.model.DefaultEntityApplicationModel;
import is.codion.framework.model.DefaultEntityModel;
import is.codion.framework.model.EntityApplicationModel;
import is.codion.framework.model.EntityEditModel;
import is.codion.framework.model.EntityModel;
import is.codion.framework.model.EntityTableModel;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A base class for testing {@link EntityApplicationModel} subclasses.
 * @param <Model> the {@link EntityModel} type
 * @param <EditModel> the {@link EntityEditModel} type
 * @param <TableModel> the {@link EntityTableModel} type
 */
public abstract class AbstractEntityApplicationModelTest<Model extends DefaultEntityModel<Model, EditModel, TableModel>,
				EditModel extends AbstractEntityEditModel, TableModel extends EntityTableModel<EditModel>> {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.user(UNIT_TEST_USER)
					.domain(new TestDomain())
					.build();

	private final EntityConnectionProvider connectionProvider;

	protected AbstractEntityApplicationModelTest() {
		this.connectionProvider = CONNECTION_PROVIDER;
	}

	@Test
	public void test() {
		EntityApplicationModel<Model, EditModel, TableModel> model = new DefaultEntityApplicationModel<>(connectionProvider);
		Model deptModel = createDepartmentModel();
		model.addEntityModel(deptModel);
		assertThrows(IllegalArgumentException.class, () -> model.addEntityModel(deptModel));
		assertNotNull(model.entityModel(Department.TYPE));
		assertEquals(1, model.entityModels().size());
		assertEquals(UNIT_TEST_USER, model.user());

		assertThrows(IllegalArgumentException.class, () -> model.entityModel(Employee.TYPE));
		if (!deptModel.containsTableModel()) {
			return;
		}
		deptModel.detailModels().get(Employee.TYPE).tableModel().queryModel().conditionRequired().set(false);
		model.refresh();
		assertTrue(deptModel.tableModel().items().visible().count() > 0);
	}

	@Test
	public void constructorNullConnectionProvider() {
		assertThrows(NullPointerException.class, () -> new DefaultEntityApplicationModel<>(null));
	}

	@Test
	public void entityModelByEntityTypeNotFound() {
		EntityApplicationModel<Model, EditModel, TableModel> model = new DefaultEntityApplicationModel<>(connectionProvider);
		assertThrows(IllegalArgumentException.class, () -> model.entityModel(Department.TYPE));
	}

	@Test
	public void entityModelByEntityType() {
		EntityApplicationModel<Model, EditModel, TableModel> model = new DefaultEntityApplicationModel<>(connectionProvider);
		Model departmentModel = createDepartmentModel();
		model.addEntityModel(departmentModel);
		assertEquals(departmentModel, model.entityModel(Department.TYPE));
	}

	@Test
	public void entityModelByClass() {
		EntityApplicationModel<Model, EditModel, TableModel> model = new DefaultEntityApplicationModel<>(connectionProvider);
		Model departmentModel = createDepartmentModel();
		assertThrows(IllegalArgumentException.class, () -> model.entityModel((Class<? extends Model>) departmentModel.getClass()));
		model.addEntityModels(departmentModel);
		assertEquals(departmentModel, model.entityModel((Class<? extends Model>) departmentModel.getClass()));
	}

	@Test
	public void containsEntityModel() {
		EntityApplicationModel<Model, EditModel, TableModel> model = new DefaultEntityApplicationModel<>(connectionProvider);
		Model departmentModel = createDepartmentModel();
		model.addEntityModel(departmentModel);

		assertTrue(model.containsEntityModel(Department.TYPE));
		assertTrue(model.containsEntityModel((Class<? extends Model>) departmentModel.getClass()));
		assertTrue(model.containsEntityModel(departmentModel));

		assertFalse(model.containsEntityModel(Employee.TYPE));
		Model detailModel = departmentModel.detailModels().get(Employee.TYPE);
		assertFalse(model.containsEntityModel(detailModel));
	}

	protected final EntityConnectionProvider connectionProvider() {
		return connectionProvider;
	}

	/**
	 * @return a EntityModel based on the department entity
	 * @see Department#TYPE
	 */
	protected abstract Model createDepartmentModel();
}
