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

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.reactive.state.State;
import is.codion.common.utilities.Conjunction;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection.Select;
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

import java.util.List;

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
		assertFalse(conditionModel.get(Employee.DEPARTMENT_FK).enabled().is());
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
		assertFalse(queryModel.conditionChanged().is());
		queryModel.limit().clear();

		queryModel.orderBy().set(OrderBy.descending(Employee.NAME));
		nameCondition.operands().equal().set(null);
		assertFalse(queryModel.conditionChanged().is());
		queryModel.orderBy().clear();

		queryModel.attributes().include().set(asList(Employee.NAME, Employee.JOB));
		nameCondition.operands().equal().set(null);
		assertFalse(queryModel.conditionChanged().is());
		queryModel.attributes().include().clear();

		nameCondition.operands().equal().set("Scott");
		assertTrue(queryModel.conditionChanged().is());

		nameCondition.clear();
		assertFalse(queryModel.conditionChanged().is());

		queryModel.where().set(Employee.CONDITION_2_TYPE::get);
		assertTrue(queryModel.conditionChanged().is());
		queryModel.query();
		assertFalse(queryModel.conditionChanged().is());
		queryModel.where().conjunction().set(Conjunction.OR);
		assertTrue(queryModel.conditionChanged().is());
		queryModel.query();
		assertFalse(queryModel.conditionChanged().is());

		queryModel = new DefaultEntityQueryModel(new DefaultEntityTableConditionModel(Job.TYPE,
						CONNECTION_PROVIDER, new EntityConditionModelFactory(Job.TYPE, CONNECTION_PROVIDER)));
		assertFalse(queryModel.conditionChanged().is());
		queryModel.having().set(Job.ADDITIONAL_HAVING::get);
		assertTrue(queryModel.conditionChanged().is());
		queryModel.query();
		assertFalse(queryModel.conditionChanged().is());
		queryModel.having().conjunction().set(Conjunction.OR);
		assertTrue(queryModel.conditionChanged().is());
		queryModel.query();
		assertFalse(queryModel.conditionChanged().is());
	}

	@Test
	void attributes() {
		DefaultEntityQueryModel queryModel = new DefaultEntityQueryModel(new DefaultEntityTableConditionModel(Employee.TYPE,
						CONNECTION_PROVIDER, new EntityConditionModelFactory(Employee.TYPE, CONNECTION_PROVIDER)));
		queryModel.limit().set(1);
		Entity employee = queryModel.query().get(0);
		assertTrue(employee.contains(Employee.NAME));
		assertTrue(employee.contains(Employee.JOB));
		assertNull(employee.get(Employee.DATA));// not selected by default
		assertTrue(employee.contains(Employee.MGR_FK));

		queryModel.attributes().include().set(asList(Employee.NAME, Employee.JOB, Employee.DATA));
		employee = queryModel.query().get(0);
		assertTrue(employee.contains(Employee.NAME));
		assertTrue(employee.contains(Employee.JOB));
		assertNotNull(employee.get(Employee.DATA)); // included manually

		queryModel.attributes().include().clear();
		queryModel.attributes().exclude().set(singleton(Employee.JOB));
		employee = queryModel.query().get(0);
		assertTrue(employee.contains(Employee.NAME));
		assertFalse(employee.contains(Employee.JOB));
		assertTrue(employee.contains(Employee.MGR));
		assertTrue(employee.contains(Employee.MGR_FK));

		queryModel.attributes().include().set(asList(Employee.NAME, Employee.JOB, Employee.MGR_FK));
		employee = queryModel.query().get(0);
		assertTrue(employee.contains(Employee.NAME));
		assertFalse(employee.contains(Employee.JOB));
		assertTrue(employee.contains(Employee.MGR));
		assertTrue(employee.contains(Employee.MGR_FK));

		queryModel.attributes().include().set(singleton(Employee.JOB));
		employee = queryModel.query().get(0);
		assertTrue(employee.contains(Employee.NAME));
		assertTrue(employee.contains(Employee.MGR));
		assertTrue(employee.contains(Employee.MGR_FK));
		assertTrue(employee.contains(Employee.COMMISSION));

		assertThrows(IllegalArgumentException.class, () -> queryModel.attributes().include().set(singleton(Department.NAME)));
		assertThrows(IllegalArgumentException.class, () -> queryModel.attributes().exclude().set(singleton(Department.NAME)));
	}

	@Test
	void orderBy() {
		// Test order by clause functionality
		DefaultEntityQueryModel queryModel = new DefaultEntityQueryModel(new DefaultEntityTableConditionModel(Employee.TYPE,
						CONNECTION_PROVIDER, new EntityConditionModelFactory(Employee.TYPE, CONNECTION_PROVIDER)));

		// Store initial order by (may have default)
		OrderBy initialOrderBy = queryModel.orderBy().get();

		// Set order by
		OrderBy orderByName = OrderBy.ascending(Employee.NAME);
		queryModel.orderBy().set(orderByName);
		assertEquals(orderByName, queryModel.orderBy().get());

		// Verify it affects the query
		queryModel.limit().set(3);
		List<Entity> results = queryModel.query();
		assertEquals(3, results.size());
		// Verify ordering by checking names are in ascending order
		String previousName = "";
		for (Entity employee : results) {
			String name = employee.get(Employee.NAME);
			assertTrue(name.compareTo(previousName) >= 0);
			previousName = name;
		}

		// Test descending order
		OrderBy orderByNameDesc = OrderBy.descending(Employee.NAME);
		queryModel.orderBy().set(orderByNameDesc);
		results = queryModel.query();
		previousName = "ZZZZZ"; // Start with high value
		for (Entity employee : results) {
			String name = employee.get(Employee.NAME);
			assertTrue(name.compareTo(previousName) <= 0);
			previousName = name;
		}

		// Test clearing order by (reverts to default/initial)
		queryModel.orderBy().clear();
		assertEquals(initialOrderBy, queryModel.orderBy().get());

		// Test multiple column order by
		OrderBy multiOrder = OrderBy.builder()
						.ascending(Employee.DEPARTMENT)
						.descending(Employee.NAME)
						.build();
		queryModel.orderBy().set(multiOrder);
		assertEquals(multiOrder, queryModel.orderBy().get());
	}

	@Test
	void limit() {
		// Test query limit functionality
		DefaultEntityQueryModel queryModel = new DefaultEntityQueryModel(new DefaultEntityTableConditionModel(Employee.TYPE,
						CONNECTION_PROVIDER, new EntityConditionModelFactory(Employee.TYPE, CONNECTION_PROVIDER)));

		// Test default limit from configuration
		Integer defaultLimit = EntityQueryModel.LIMIT.get();
		assertEquals(defaultLimit, queryModel.limit().get());

		// Set specific limit
		queryModel.limit().set(5);
		assertEquals(5, queryModel.limit().get());
		List<Entity> results = queryModel.query();
		assertTrue(results.size() <= 5);

		// Test with limit 0 (should return empty)
		queryModel.limit().set(0);
		results = queryModel.query();
		assertTrue(results.isEmpty());

		// Test with null limit (fetch all)
		queryModel.limit().clear();
		assertNull(queryModel.limit().get());
		results = queryModel.query();
		// Should have all employees (assuming test data has more than 5)
		assertTrue(results.size() > 5);

		// Test that limit affects query results
		queryModel.limit().set(10);
		results = queryModel.query();
		assertTrue(results.size() <= 10);
	}

	@Test
	void conditionRequired() {
		// Test condition required functionality - prevents fetching all rows accidentally
		DefaultEntityQueryModel queryModel = new DefaultEntityQueryModel(new DefaultEntityTableConditionModel(Employee.TYPE,
						CONNECTION_PROVIDER, new EntityConditionModelFactory(Employee.TYPE, CONNECTION_PROVIDER)));

		// Default should be false
		assertFalse(queryModel.conditionRequired().is());

		// With no condition and not required, should fetch all
		queryModel.limit().set(100); // Set high limit to ensure we get results
		List<Entity> results = queryModel.query();
		assertFalse(results.isEmpty());
		int allRowsCount = results.size();

		// Enable condition required
		queryModel.conditionRequired().set(true);
		assertTrue(queryModel.conditionRequired().is());

		// Override conditionEnabled to require the NAME condition specifically
		queryModel.conditionEnabled().set(queryModel.condition().get(Employee.NAME).enabled());

		// Now with NAME condition not enabled, should return empty
		results = queryModel.query();
		assertTrue(results.isEmpty());

		// Set a value in the NAME condition
		queryModel.condition().get(Employee.NAME).operands().equal().set("SCOTT");
		results = queryModel.query();
		assertFalse(results.isEmpty());
		assertTrue(results.size() < allRowsCount); // Should have fewer results

		// Clear the NAME condition value - should return empty again
		queryModel.condition().get(Employee.NAME).clear();
		results = queryModel.query();
		assertTrue(results.isEmpty());

		// Test with custom conditionEnabled logic - require either NAME or DEPARTMENT
		queryModel.conditionEnabled().set(State.or(
						queryModel.condition().get(Employee.NAME).enabled(),
						queryModel.condition().get(Employee.DEPARTMENT).enabled()
		));

		// Still empty (neither condition has values)
		results = queryModel.query();
		assertTrue(results.isEmpty());

		// Add department condition
		queryModel.condition().get(Employee.DEPARTMENT).set().in(10, 20);
		results = queryModel.query();
		assertFalse(results.isEmpty()); // Now works because DEPARTMENT is enabled

		// Clear conditions and disable conditionRequired
		queryModel.condition().get(Employee.DEPARTMENT).clear();
		queryModel.conditionRequired().set(false);
		results = queryModel.query();
		assertEquals(allRowsCount, results.size()); // Back to all rows
	}

	@Test
	void having() {
		// Test HAVING clause functionality (for aggregate queries)
		DefaultEntityQueryModel queryModel = new DefaultEntityQueryModel(new DefaultEntityTableConditionModel(Job.TYPE,
						CONNECTION_PROVIDER, new EntityConditionModelFactory(Job.TYPE, CONNECTION_PROVIDER)));

		// Job entity has aggregate columns (MAX_SALARY, MIN_SALARY, etc.)
		// Initial query should return all job groups
		List<Entity> results = queryModel.query();
		int initialCount = results.size();
		assertTrue(initialCount > 0);

		// Add HAVING condition
		queryModel.having().set(() -> Job.MAX_SALARY.greaterThan(5000.0));
		results = queryModel.query();
		assertTrue(results.size() < initialCount); // Should filter out some jobs

		// Check conjunction
		assertEquals(Conjunction.AND, queryModel.having().conjunction().get());

		// Change to OR conjunction (though with single condition it doesn't matter)
		queryModel.having().conjunction().set(Conjunction.OR);
		assertEquals(Conjunction.OR, queryModel.having().conjunction().get());

		// Add another HAVING condition using the Job.ADDITIONAL_HAVING ConditionType
		queryModel.having().set(Job.ADDITIONAL_HAVING::get);
		queryModel.query();

		// Clear having
		queryModel.having().clear();
		results = queryModel.query();
		assertEquals(initialCount, results.size()); // Back to original count

		// Test that having is included in the Select
		queryModel.having().set(() -> Job.MIN_SALARY.greaterThan(1000.0));
		Select select = queryModel.select();
		assertNotNull(select.having());
	}

	@Test
	void complexConditions() {
		// Test complex WHERE conditions combining table conditions and additional where
		DefaultEntityQueryModel queryModel = new DefaultEntityQueryModel(new DefaultEntityTableConditionModel(Employee.TYPE,
						CONNECTION_PROVIDER, new EntityConditionModelFactory(Employee.TYPE, CONNECTION_PROVIDER)));

		// Set a limit to make results predictable
		queryModel.limit().set(100);

		// Get baseline count
		int allCount = queryModel.query().size();

		// Add table condition
		queryModel.condition().get(Employee.DEPARTMENT).set().in(10, 20, 30);
		int withDeptCondition = queryModel.query().size();
		assertTrue(withDeptCondition <= allCount);

		// Add additional WHERE condition (should AND with table conditions by default)
		queryModel.where().set(() -> Employee.JOB.in("CLERK", "MANAGER"));
		int withBothConditions = queryModel.query().size();
		assertTrue(withBothConditions <= withDeptCondition);

		// Change conjunction to OR
		queryModel.where().conjunction().set(Conjunction.OR);
		int withOrConditions = queryModel.query().size();
		// OR should give more or equal results than AND
		assertTrue(withOrConditions >= withBothConditions);

		// Test with condition supplier that returns null
		queryModel.where().set(() -> null);
		int withNullAdditional = queryModel.query().size();
		assertEquals(withDeptCondition, withNullAdditional); // Should be same as just dept condition

		// Test conditionChanged state
		queryModel.query(); // Reset conditionChanged
		assertFalse(queryModel.conditionChanged().is());

		// Change condition
		queryModel.condition().get(Employee.NAME).set().in("SCOTT", "KING");
		assertTrue(queryModel.conditionChanged().is());

		// Query resets conditionChanged
		queryModel.query();
		assertFalse(queryModel.conditionChanged().is());
	}
}
