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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.Conjunction;
import is.codion.common.model.condition.ConditionModel;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.domain.entity.condition.Condition.Combination;
import is.codion.framework.model.test.TestDomain;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;
import is.codion.framework.model.test.TestDomain.Job;

import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.*;

public final class DefaultEntityQueryModelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	@Test
	void condition() {
		DefaultEntityQueryModel queryModel = new DefaultEntityQueryModel(new DefaultEntityTableConditionModel(Employee.TYPE,
						CONNECTION_PROVIDER, new EntityConditionModelFactory(Employee.TYPE, CONNECTION_PROVIDER)));
		EntityTableConditionModel conditionModel = queryModel.condition();
		assertFalse(conditionModel.get(Employee.DEPARTMENT_FK).enabled().get());
		conditionModel.get(Employee.NAME).set().in("Scott", "John");
		Condition condition = queryModel.select().where();
		assertFalse(condition instanceof Combination);
		queryModel.where().set(Employee.CONDITION_2_TYPE::get);
		assertNotNull(queryModel.where().get());
		condition = queryModel.select().where();
		assertInstanceOf(Combination.class, condition);
		assertEquals(Conjunction.AND, ((Combination) condition).conjunction());
		queryModel.where().conjunction().set(Conjunction.OR);
		condition = queryModel.select().where();
		assertEquals(Conjunction.OR, ((Combination) condition).conjunction());
		queryModel.where().set(null);
		condition = queryModel.select().where();
		assertFalse(condition instanceof Combination);
	}

	@Test
	void conditionChanged() {
		DefaultEntityQueryModel queryModel = new DefaultEntityQueryModel(new DefaultEntityTableConditionModel(Employee.TYPE,
						CONNECTION_PROVIDER, new EntityConditionModelFactory(Employee.TYPE, CONNECTION_PROVIDER)));

		ConditionModel<String> nameCondition = queryModel.condition().get(Employee.NAME);

		queryModel.limit().set(10);
		nameCondition.operands().equal().set(null);
		assertFalse(queryModel.conditionChanged().get());
		queryModel.limit().clear();

		queryModel.orderBy().set(OrderBy.descending(Employee.NAME));
		nameCondition.operands().equal().set(null);
		assertFalse(queryModel.conditionChanged().get());
		queryModel.orderBy().clear();

		queryModel.attributes().included().set(asList(Employee.NAME, Employee.JOB));
		nameCondition.operands().equal().set(null);
		assertFalse(queryModel.conditionChanged().get());
		queryModel.attributes().included().clear();

		nameCondition.operands().equal().set("Scott");
		assertTrue(queryModel.conditionChanged().get());

		nameCondition.clear();
		assertFalse(queryModel.conditionChanged().get());

		queryModel.where().set(Employee.CONDITION_2_TYPE::get);
		assertTrue(queryModel.conditionChanged().get());
		queryModel.query();
		assertFalse(queryModel.conditionChanged().get());
		queryModel.where().conjunction().set(Conjunction.OR);
		assertTrue(queryModel.conditionChanged().get());
		queryModel.query();
		assertFalse(queryModel.conditionChanged().get());

		queryModel = new DefaultEntityQueryModel(new DefaultEntityTableConditionModel(Job.TYPE,
						CONNECTION_PROVIDER, new EntityConditionModelFactory(Job.TYPE, CONNECTION_PROVIDER)));
		assertFalse(queryModel.conditionChanged().get());
		queryModel.having().set(Job.ADDITIONAL_HAVING::get);
		assertTrue(queryModel.conditionChanged().get());
		queryModel.query();
		assertFalse(queryModel.conditionChanged().get());
		queryModel.having().conjunction().set(Conjunction.OR);
		assertTrue(queryModel.conditionChanged().get());
		queryModel.query();
		assertFalse(queryModel.conditionChanged().get());
	}

	@Test
	void attributes() {
		DefaultEntityQueryModel queryModel = new DefaultEntityQueryModel(new DefaultEntityTableConditionModel(Employee.TYPE,
						CONNECTION_PROVIDER, new EntityConditionModelFactory(Employee.TYPE, CONNECTION_PROVIDER)));
		queryModel.limit().set(1);
		queryModel.attributes().included().set(asList(Employee.NAME, Employee.JOB));
		Entity employee = queryModel.query().get(0);
		assertTrue(employee.contains(Employee.NAME));
		assertTrue(employee.contains(Employee.JOB));
		assertFalse(employee.contains(Employee.MGR));
		assertFalse(employee.contains(Employee.MGR_FK));

		queryModel.attributes().included().clear();
		queryModel.attributes().excluded().set(singleton(Employee.JOB));
		employee = queryModel.query().get(0);
		assertTrue(employee.contains(Employee.NAME));
		assertFalse(employee.contains(Employee.JOB));
		assertTrue(employee.contains(Employee.MGR));
		assertTrue(employee.contains(Employee.MGR_FK));

		queryModel.attributes().included().set(asList(Employee.NAME, Employee.JOB, Employee.MGR_FK));
		employee = queryModel.query().get(0);
		assertTrue(employee.contains(Employee.NAME));
		assertFalse(employee.contains(Employee.JOB));
		assertTrue(employee.contains(Employee.MGR));
		assertTrue(employee.contains(Employee.MGR_FK));
		assertFalse(employee.contains(Employee.COMMISSION));

		queryModel.attributes().included().set(singleton(Employee.JOB));
		employee = queryModel.query().get(0);
		// All, since included and excluded cancel each other out
		assertTrue(employee.contains(Employee.NAME));
		assertTrue(employee.contains(Employee.JOB));
		assertTrue(employee.contains(Employee.MGR));
		assertTrue(employee.contains(Employee.MGR_FK));
		assertTrue(employee.contains(Employee.COMMISSION));

		assertThrows(IllegalArgumentException.class, () -> queryModel.attributes().included().set(singleton(Department.NAME)));
		assertThrows(IllegalArgumentException.class, () -> queryModel.attributes().excluded().set(singleton(Department.NAME)));
	}
}
