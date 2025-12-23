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
 * Copyright (c) 2016 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.utilities.Operator;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.model.test.TestDomain;
import is.codion.framework.model.test.TestDomain.DateTimeTest;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Detail;
import is.codion.framework.model.test.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultEntityConditionModelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	private final EntityConditionModel conditionModel = EntityConditionModel.builder()
					.entityType(Employee.TYPE)
					.connectionProvider(CONNECTION_PROVIDER)
					.conditions(new EntityConditions(Employee.TYPE, CONNECTION_PROVIDER))
					.build();

	@Test
	void test() {
		assertEquals(Employee.TYPE, conditionModel.entityType());
		assertEquals(11, conditionModel.get().size());

		assertFalse(conditionModel.get(Employee.DEPARTMENT_FK).enabled().is());

		assertFalse(conditionModel.enabled().is());
		conditionModel.get(Employee.DEPARTMENT_FK).enabled().set(true);
		assertTrue(conditionModel.enabled().is());
	}

	@Test
	void noSearchColumnsDefined() {
		EntityConditionModel model = EntityConditionModel.builder()
						.entityType(Detail.TYPE)
						.connectionProvider(CONNECTION_PROVIDER)
						.conditions(new EntityConditions(Detail.TYPE, CONNECTION_PROVIDER))
						.build();
		//no search columns defined for master entity
		ForeignKeyConditionModel masterModel = model.get(Detail.MASTER_FK);
		assertThrows(IllegalStateException.class, () ->
						masterModel.equalSearchModel().search().result());
	}

	@Test
	void conditionModel() {
		assertNotNull(conditionModel.get(Employee.COMMISSION));
	}

	@Test
	void conditionModelNonExisting() {
		assertThrows(IllegalArgumentException.class, () -> conditionModel.get(Department.ID));
	}

	@Test
	void setEqualOperand() {
		Entity sales = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME.equalTo("SALES"));
		assertFalse(conditionModel.get(Employee.DEPARTMENT_FK).enabled().is());
		boolean searchStateChanged = conditionModel.get(Employee.DEPARTMENT_FK).set().equalTo(sales);
		assertTrue(searchStateChanged);
		assertTrue(conditionModel.get(Employee.DEPARTMENT_FK).enabled().is());
		ForeignKeyConditionModel deptModel =
						conditionModel.get(Employee.DEPARTMENT_FK);
		assertSame(deptModel.operands().equal().get(), sales);
		searchStateChanged = conditionModel.get(Employee.DEPARTMENT_FK).set().equalTo(null);
		assertTrue(searchStateChanged);
		assertFalse(conditionModel.get(Employee.DEPARTMENT_FK).enabled().is());
	}

	@Test
	void setInOperands() {
		Entity sales = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME.equalTo("SALES"));
		Entity accounting = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME.equalTo("ACCOUNTING"));
		assertFalse(conditionModel.get(Employee.DEPARTMENT_FK).enabled().is());
		boolean searchStateChanged = conditionModel.get(Employee.DEPARTMENT_FK).set().in(sales, accounting);
		assertTrue(searchStateChanged);
		assertTrue(conditionModel.get(Employee.DEPARTMENT_FK).enabled().is());
		ForeignKeyConditionModel deptModel = conditionModel.get(Employee.DEPARTMENT_FK);
		assertTrue(deptModel.operands().in().get().contains(sales));
		assertTrue(deptModel.operands().in().get().contains(accounting));
		searchStateChanged = conditionModel.get(Employee.DEPARTMENT_FK).set().in(emptyList());
		assertTrue(searchStateChanged);
		assertFalse(conditionModel.get(Employee.DEPARTMENT_FK).enabled().is());
		conditionModel.get(Employee.DEPARTMENT_FK).enabled().set(true);
		assertEquals(Employee.DEPARTMENT_FK.isNull(), conditionModel.where());
		conditionModel.get(Employee.DEPARTMENT_FK).operator().set(Operator.NOT_IN);
		conditionModel.get(Employee.DEPARTMENT_FK).enabled().set(true);
		assertEquals(Employee.DEPARTMENT_FK.isNotNull(), conditionModel.where());
	}

	@Test
	void conditions() {
		ConditionModel<Double> condition = conditionModel.get(Employee.COMMISSION);
		condition.set().between(0d, null);
		condition.enabled().set(true);
		assertEquals(Employee.COMMISSION.greaterThanOrEqualTo(0d), conditionModel.where());
		condition.set().between(null, 0d);
		condition.enabled().set(true);
		assertEquals(Employee.COMMISSION.lessThanOrEqualTo(0d), conditionModel.where());
		condition.set().betweenExclusive(0d, null);
		condition.enabled().set(true);
		assertEquals(Employee.COMMISSION.greaterThan(0d), conditionModel.where());
		condition.set().betweenExclusive(null, 0d);
		condition.enabled().set(true);
		assertEquals(Employee.COMMISSION.lessThan(0d), conditionModel.where());

		condition.set().notBetween(0d, null);
		condition.enabled().set(true);
		assertEquals(Employee.COMMISSION.lessThanOrEqualTo(0d), conditionModel.where());
		condition.set().notBetween(null, 0d);
		condition.enabled().set(true);
		assertEquals(Employee.COMMISSION.greaterThanOrEqualTo(0d), conditionModel.where());
		condition.set().notBetweenExclusive(0d, null);
		condition.enabled().set(true);
		assertEquals(Employee.COMMISSION.lessThan(0d), conditionModel.where());
		condition.set().notBetweenExclusive(null, 0d);
		condition.enabled().set(true);
		assertEquals(Employee.COMMISSION.greaterThan(0d), conditionModel.where());
	}

	@Test
	void clearConditions() {
		Entity sales = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME.equalTo("SALES"));
		Entity accounting = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME.equalTo("ACCOUNTING"));
		assertFalse(conditionModel.get(Employee.DEPARTMENT_FK).enabled().is());
		conditionModel.get(Employee.DEPARTMENT_FK).set().in(sales, accounting);
		assertTrue(conditionModel.get(Employee.DEPARTMENT_FK).enabled().is());
		conditionModel.clear();
		assertFalse(conditionModel.get(Employee.DEPARTMENT_FK).enabled().is());
	}

	@Test
	void dateTimeEqualTo() {
		EntityConditionModel condition = EntityConditionModel.builder()
						.entityType(DateTimeTest.TYPE)
						.connectionProvider(CONNECTION_PROVIDER)
						.build();
		EntityDefinition entityDefinition = CONNECTION_PROVIDER.entities().definition(DateTimeTest.TYPE);

		ConditionModel<LocalTime> timeConditionModel = condition.get(DateTimeTest.TIME_HH_MM);
		timeConditionModel.set().equalTo(LocalTime.of(11, 00));
		Condition where = condition.where();
		assertEquals("(time_hh_mm >= ? AND time_hh_mm < ?)", where.string(entityDefinition));
		assertEquals(2, where.values().size());
		assertEquals(LocalTime.of(11, 00), where.values().get(0));
		assertEquals(LocalTime.of(11, 01), where.values().get(1));

		condition = EntityConditionModel.builder()
						.entityType(DateTimeTest.TYPE)
						.connectionProvider(CONNECTION_PROVIDER)
						.build();

		timeConditionModel = condition.get(DateTimeTest.TIME_HH_MM_SS);
		timeConditionModel.set().equalTo(LocalTime.of(11, 00, 2));
		where = condition.where();
		assertEquals("(time_hh_mm_ss >= ? AND time_hh_mm_ss < ?)", where.string(entityDefinition));
		assertEquals(2, where.values().size());
		assertEquals(LocalTime.of(11, 00, 2), where.values().get(0));
		assertEquals(LocalTime.of(11, 00, 3), where.values().get(1));

		condition = EntityConditionModel.builder()
						.entityType(DateTimeTest.TYPE)
						.connectionProvider(CONNECTION_PROVIDER)
						.build();

		timeConditionModel = condition.get(DateTimeTest.TIME_HH_MM_SS_SSS);
		timeConditionModel.set().equalTo(LocalTime.of(11, 00, 3, 999_000_000));
		where = condition.where();
		assertEquals("time_hh_mm_ss_sss = ?", where.string(entityDefinition));
		assertEquals(1, where.values().size());
		assertEquals(LocalTime.of(11, 00, 3, 999_000_000), where.values().get(0));

		condition = EntityConditionModel.builder()
						.entityType(DateTimeTest.TYPE)
						.connectionProvider(CONNECTION_PROVIDER)
						.build();

		ConditionModel<LocalDateTime> dateTimeConditionModel = condition.get(DateTimeTest.DATE_TIME_HH_MM);
		dateTimeConditionModel.set().equalTo(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 45));
		where = condition.where();
		assertEquals("(date_time_hh_mm >= ? AND date_time_hh_mm < ?)", where.string(entityDefinition));
		assertEquals(2, where.values().size());
		assertEquals(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 45), where.values().get(0));
		assertEquals(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 46), where.values().get(1));

		condition = EntityConditionModel.builder()
						.entityType(DateTimeTest.TYPE)
						.connectionProvider(CONNECTION_PROVIDER)
						.build();

		dateTimeConditionModel = condition.get(DateTimeTest.DATE_TIME_HH_MM_SS);
		dateTimeConditionModel.set().equalTo(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 45, 15));
		where = condition.where();
		assertEquals("(date_time_hh_mm_ss >= ? AND date_time_hh_mm_ss < ?)", where.string(entityDefinition));
		assertEquals(2, where.values().size());
		assertEquals(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 45, 15), where.values().get(0));
		assertEquals(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 45, 16), where.values().get(1));

		condition = EntityConditionModel.builder()
						.entityType(DateTimeTest.TYPE)
						.connectionProvider(CONNECTION_PROVIDER)
						.build();

		dateTimeConditionModel = condition.get(DateTimeTest.DATE_TIME_HH_MM_SS_SSS);
		dateTimeConditionModel.set().equalTo(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 45, 15, 999_000_000));
		where = condition.where();
		assertEquals("date_time_hh_mm_ss_sss = ?", where.string(entityDefinition));
		assertEquals(1, where.values().size());
		assertEquals(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 45, 15, 999_000_000), where.values().get(0));
	}

	@Test
	void dateTimeNotEqualTo() {
		EntityConditionModel condition = EntityConditionModel.builder()
						.entityType(DateTimeTest.TYPE)
						.connectionProvider(CONNECTION_PROVIDER)
						.build();
		EntityDefinition entityDefinition = CONNECTION_PROVIDER.entities().definition(DateTimeTest.TYPE);

		ConditionModel<LocalTime> timeConditionModel = condition.get(DateTimeTest.TIME_HH_MM);
		timeConditionModel.set().notEqualTo(LocalTime.of(11, 00));
		Condition where = condition.where();
		assertEquals("(time_hh_mm < ? OR time_hh_mm >= ?)", where.string(entityDefinition));
		assertEquals(2, where.values().size());
		assertEquals(LocalTime.of(11, 00), where.values().get(0));
		assertEquals(LocalTime.of(11, 01), where.values().get(1));

		condition = EntityConditionModel.builder()
						.entityType(DateTimeTest.TYPE)
						.connectionProvider(CONNECTION_PROVIDER)
						.build();

		timeConditionModel = condition.get(DateTimeTest.TIME_HH_MM_SS);
		timeConditionModel.set().notEqualTo(LocalTime.of(11, 00, 2));
		where = condition.where();
		assertEquals("(time_hh_mm_ss < ? OR time_hh_mm_ss >= ?)", where.string(entityDefinition));
		assertEquals(2, where.values().size());
		assertEquals(LocalTime.of(11, 00, 2), where.values().get(0));
		assertEquals(LocalTime.of(11, 00, 3), where.values().get(1));

		condition = EntityConditionModel.builder()
						.entityType(DateTimeTest.TYPE)
						.connectionProvider(CONNECTION_PROVIDER)
						.build();

		timeConditionModel = condition.get(DateTimeTest.TIME_HH_MM_SS_SSS);
		timeConditionModel.set().notEqualTo(LocalTime.of(11, 00, 3, 999_000_000));
		where = condition.where();
		assertEquals("time_hh_mm_ss_sss <> ?", where.string(entityDefinition));
		assertEquals(1, where.values().size());
		assertEquals(LocalTime.of(11, 00, 3, 999_000_000), where.values().get(0));

		condition = EntityConditionModel.builder()
						.entityType(DateTimeTest.TYPE)
						.connectionProvider(CONNECTION_PROVIDER)
						.build();

		ConditionModel<LocalDateTime> dateTimeConditionModel = condition.get(DateTimeTest.DATE_TIME_HH_MM);
		dateTimeConditionModel.set().notEqualTo(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 45));
		where = condition.where();
		assertEquals("(date_time_hh_mm < ? OR date_time_hh_mm >= ?)", where.string(entityDefinition));
		assertEquals(2, where.values().size());
		assertEquals(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 45), where.values().get(0));
		assertEquals(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 46), where.values().get(1));

		condition = EntityConditionModel.builder()
						.entityType(DateTimeTest.TYPE)
						.connectionProvider(CONNECTION_PROVIDER)
						.build();

		dateTimeConditionModel = condition.get(DateTimeTest.DATE_TIME_HH_MM_SS);
		dateTimeConditionModel.set().notEqualTo(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 45, 15));
		where = condition.where();
		assertEquals("(date_time_hh_mm_ss < ? OR date_time_hh_mm_ss >= ?)", where.string(entityDefinition));
		assertEquals(2, where.values().size());
		assertEquals(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 45, 15), where.values().get(0));
		assertEquals(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 45, 16), where.values().get(1));

		condition = EntityConditionModel.builder()
						.entityType(DateTimeTest.TYPE)
						.connectionProvider(CONNECTION_PROVIDER)
						.build();

		dateTimeConditionModel = condition.get(DateTimeTest.DATE_TIME_HH_MM_SS_SSS);
		dateTimeConditionModel.set().notEqualTo(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 45, 15, 999_000_000));
		where = condition.where();
		assertEquals("date_time_hh_mm_ss_sss <> ?", where.string(entityDefinition));
		assertEquals(1, where.values().size());
		assertEquals(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 45, 15, 999_000_000), where.values().get(0));
	}
}
