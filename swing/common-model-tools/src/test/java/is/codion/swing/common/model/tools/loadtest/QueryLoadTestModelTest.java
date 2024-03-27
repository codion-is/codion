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
package is.codion.swing.common.model.tools.loadtest;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.model.loadtest.LoadTest.Scenario;
import is.codion.common.user.User;
import is.codion.swing.common.model.tools.loadtest.QueryLoadTestModel.QueryApplication;
import is.codion.swing.common.model.tools.loadtest.QueryLoadTestModel.QueryPerformer;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class QueryLoadTestModelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final Scenario<QueryApplication> SELECT_EMPLOYEE =
					Scenario.builder(new QueryPerformer(UNIT_TEST_USER, "select * from employees.employee where ename not like ?"))
									.name("selectEmployees")
									.build();
	private static final Scenario<QueryApplication> SELECT_DEPARTMENTS =
					Scenario.builder(new QueryPerformer(UNIT_TEST_USER, "select * from employees.department", true))
									.name("selectDepartments")
									.build();

	@Test
	void test() throws DatabaseException {
		QueryLoadTestModel queryLoadTest = new QueryLoadTestModel(Database.instance(), UNIT_TEST_USER,
						asList(SELECT_DEPARTMENTS, SELECT_EMPLOYEE));
		Map<String, AtomicInteger> counters = new HashMap<>();
		queryLoadTest.loadTest().addResultListener(result ->
						counters.computeIfAbsent(result.scenario(), scenarioName -> new AtomicInteger()).incrementAndGet());
		queryLoadTest.loadTest().minimumThinkTime().set(10);
		queryLoadTest.loadTest().maximumThinkTime().set(30);
		queryLoadTest.loadTest().loginDelayFactor().set(1);
		queryLoadTest.loadTest().applicationBatchSize().set(6);
		queryLoadTest.loadTest().addApplicationBatch();
		try {
			Thread.sleep(1500);
		}
		catch (InterruptedException ignored) {/*ignored*/}
		queryLoadTest.loadTest().removeApplicationBatch();
		try {
			Thread.sleep(500);
		}
		catch (InterruptedException ignored) {/*ignored*/}
		assertTrue(counters.containsKey(SELECT_DEPARTMENTS.name()));
		assertTrue(counters.get(SELECT_DEPARTMENTS.name()).get() > 0);
		assertTrue(counters.get(SELECT_EMPLOYEE.name()).get() > 0);
		queryLoadTest.loadTest().shutdown();
	}
}
