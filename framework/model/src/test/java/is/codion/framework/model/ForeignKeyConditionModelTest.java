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
 * Copyright (c) 2011 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.test.TestDomain;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class ForeignKeyConditionModelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	@Test
	void inSearchModel() throws DatabaseException {
		ForeignKeyConditionModel condition = ForeignKeyConditionModel.builder(Employee.DEPARTMENT_FK)
						.includeEqualOperators(EntitySearchModel.builder(Department.TYPE, CONNECTION_PROVIDER).build())
						.includeInOperators(EntitySearchModel.builder(Department.TYPE, CONNECTION_PROVIDER).build())
						.build();
		EntitySearchModel inSearchModel = condition.inSearchModel();
		Entity sales = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME.equalTo("SALES"));
		inSearchModel.selection().entity().set(sales);
		Collection<Entity> searchEntities = condition.operands().in().get();
		assertEquals(1, searchEntities.size());
		assertTrue(searchEntities.contains(sales));
		Entity accounting = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME.equalTo("ACCOUNTING"));
		List<Entity> salesAccounting = asList(sales, accounting);
		inSearchModel.selection().entities().set(salesAccounting);
		assertTrue(condition.operands().in().get().contains(sales));
		assertTrue(condition.operands().in().get().contains(accounting));
		searchEntities = condition.operands().in().get();
		assertEquals(2, searchEntities.size());
		assertTrue(searchEntities.contains(sales));
		assertTrue(searchEntities.contains(accounting));
	}

	@Test
	void equalSearchModel() throws DatabaseException {
		ForeignKeyConditionModel condition = ForeignKeyConditionModel.builder(Employee.DEPARTMENT_FK)
						.includeEqualOperators(EntitySearchModel.builder(Department.TYPE, CONNECTION_PROVIDER).build())
						.includeInOperators(EntitySearchModel.builder(Department.TYPE, CONNECTION_PROVIDER).build())
						.build();
		EntitySearchModel equalSearchModel = condition.equalSearchModel();
		Entity sales = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME.equalTo("SALES"));
		equalSearchModel.selection().entity().set(sales);
		Entity searchEntity = condition.operands().equal().get();
		assertSame(sales, searchEntity);
		Entity accounting = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME.equalTo("ACCOUNTING"));
		equalSearchModel.selection().entity().set(accounting);
		assertSame(accounting, condition.operands().equal().get());

		equalSearchModel.selection().entity().clear();

		searchEntity = condition.operands().equal().get();
		assertNull(searchEntity);

		condition.operands().equal().set(sales);
		assertEquals("SALES", equalSearchModel.searchString().get());
		sales.put(Department.NAME, "sales");
		equalSearchModel.selection().entity().set(sales);
		sales.put(Department.NAME, "SAles");
		condition.operands().equal().set(sales);
		assertEquals("SAles", equalSearchModel.searchString().get());
	}
}
