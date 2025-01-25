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
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.model.test;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.model.AbstractEntityEditModel;
import is.codion.framework.model.DefaultEntityApplicationModel;
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
 * @param <EditModel> the {@link EntityEditModel} type
 * @param <TableModel> the {@link EntityTableModel} type
 */
public abstract class AbstractEntityApplicationModelTest<EditModel extends AbstractEntityEditModel, TableModel extends EntityTableModel<EditModel>> {

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
		EntityApplicationModel<EditModel, TableModel> model = new DefaultEntityApplicationModel<>(connectionProvider);
		EntityModel<EditModel, TableModel> deptModel = createDepartmentModel();
		model.entityModels().add(deptModel);
		assertThrows(IllegalArgumentException.class, () -> model.entityModels().add(deptModel));
		assertNotNull(model.entityModels().get(Department.TYPE));
		assertEquals(1, model.entityModels().get().size());
		assertEquals(UNIT_TEST_USER, model.user());

		assertThrows(IllegalArgumentException.class, () -> model.entityModels().get(Employee.TYPE));
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
		EntityApplicationModel<EditModel, TableModel> model = new DefaultEntityApplicationModel<>(connectionProvider);
		assertThrows(IllegalArgumentException.class, () -> model.entityModels().get(Department.TYPE));
	}

	@Test
	public void entityModelByEntityType() {
		EntityApplicationModel<EditModel, TableModel> model = new DefaultEntityApplicationModel<>(connectionProvider);
		EntityModel<EditModel, TableModel> departmentModel = createDepartmentModel();
		model.entityModels().add(departmentModel);
		assertEquals(departmentModel, model.entityModels().get(Department.TYPE));
	}

	@Test
	public void entityModelByClass() {
		EntityApplicationModel<EditModel, TableModel> model = new DefaultEntityApplicationModel<>(connectionProvider);
		EntityModel<EditModel, TableModel> departmentModel = createDepartmentModel();
		assertThrows(IllegalArgumentException.class, () -> model.entityModels().get((Class<? extends EntityModel<EditModel, TableModel>>) departmentModel.getClass()));
		model.entityModels().add(departmentModel);
		assertEquals(departmentModel, model.entityModels().get((Class<? extends EntityModel<EditModel, TableModel>>) departmentModel.getClass()));
	}

	@Test
	public void containsEntityModel() {
		EntityApplicationModel<EditModel, TableModel> model = new DefaultEntityApplicationModel<>(connectionProvider);
		EntityModel<EditModel, TableModel> departmentModel = createDepartmentModel();
		model.entityModels().add(departmentModel);

		assertTrue(model.entityModels().contains(Department.TYPE));
		assertTrue(model.entityModels().contains((Class<? extends EntityModel<EditModel, TableModel>>) departmentModel.getClass()));
		assertTrue(model.entityModels().contains(departmentModel));

		assertFalse(model.entityModels().contains(Employee.TYPE));
		EntityModel<EditModel, TableModel> detailModel = departmentModel.detailModels().get(Employee.TYPE);
		assertFalse(model.entityModels().contains(detailModel));
	}

	protected final EntityConnectionProvider connectionProvider() {
		return connectionProvider;
	}

	/**
	 * @return a EntityModel based on the department entity
	 * @see Department#TYPE
	 */
	protected abstract EntityModel<EditModel, TableModel> createDepartmentModel();
}
