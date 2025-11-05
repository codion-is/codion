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
import is.codion.common.utilities.Conjunction;
import is.codion.common.utilities.Operator;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Column;
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

public class DefaultEntityTableConditionModelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	private final EntityTableConditionModel conditionModel = new DefaultEntityTableConditionModel(Employee.TYPE,
					CONNECTION_PROVIDER, new EntityConditionModelFactory(Employee.TYPE, CONNECTION_PROVIDER));

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
		EntityTableConditionModel model = new DefaultEntityTableConditionModel(Detail.TYPE,
						CONNECTION_PROVIDER, new EntityConditionModelFactory(Detail.TYPE, CONNECTION_PROVIDER));
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
		assertEquals(Employee.DEPARTMENT_FK.isNull(), conditionModel.where(Conjunction.AND));
		conditionModel.get(Employee.DEPARTMENT_FK).operator().set(Operator.NOT_IN);
		conditionModel.get(Employee.DEPARTMENT_FK).enabled().set(true);
		assertEquals(Employee.DEPARTMENT_FK.isNotNull(), conditionModel.where(Conjunction.AND));
	}

	@Test
	void conditions() {
		ConditionModel<Double> condition = conditionModel.get(Employee.COMMISSION);
		condition.set().between(0d, null);
		condition.enabled().set(true);
		assertEquals(Employee.COMMISSION.greaterThanOrEqualTo(0d), conditionModel.where(Conjunction.AND));
		condition.set().between(null, 0d);
		condition.enabled().set(true);
		assertEquals(Employee.COMMISSION.lessThanOrEqualTo(0d), conditionModel.where(Conjunction.AND));
		condition.set().betweenExclusive(0d, null);
		condition.enabled().set(true);
		assertEquals(Employee.COMMISSION.greaterThan(0d), conditionModel.where(Conjunction.AND));
		condition.set().betweenExclusive(null, 0d);
		condition.enabled().set(true);
		assertEquals(Employee.COMMISSION.lessThan(0d), conditionModel.where(Conjunction.AND));

		condition.set().notBetween(0d, null);
		condition.enabled().set(true);
		assertEquals(Employee.COMMISSION.lessThanOrEqualTo(0d), conditionModel.where(Conjunction.AND));
		condition.set().notBetween(null, 0d);
		condition.enabled().set(true);
		assertEquals(Employee.COMMISSION.greaterThanOrEqualTo(0d), conditionModel.where(Conjunction.AND));
		condition.set().notBetweenExclusive(0d, null);
		condition.enabled().set(true);
		assertEquals(Employee.COMMISSION.lessThan(0d), conditionModel.where(Conjunction.AND));
		condition.set().notBetweenExclusive(null, 0d);
		condition.enabled().set(true);
		assertEquals(Employee.COMMISSION.greaterThan(0d), conditionModel.where(Conjunction.AND));
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
		EntityTableConditionModel tableConditionModel = new DefaultEntityTableConditionModel(DateTimeTest.TYPE, CONNECTION_PROVIDER,
						new TimeTestConditionModelFactory("HH:mm"));
		EntityDefinition entityDefinition = CONNECTION_PROVIDER.entities().definition(DateTimeTest.TYPE);

		ConditionModel<LocalTime> timeConditionModel = tableConditionModel.get(DateTimeTest.TIME);
		timeConditionModel.set().equalTo(LocalTime.of(11, 00));
		Condition where = tableConditionModel.where(Conjunction.AND);
		assertEquals("(time >= ? AND time <= ?)", where.string(entityDefinition));
		assertEquals(2, where.values().size());
		assertEquals(LocalTime.of(11, 00), where.values().get(0));
		assertEquals(LocalTime.of(11, 00, 59, 999_000_000), where.values().get(1));

		tableConditionModel = new DefaultEntityTableConditionModel(DateTimeTest.TYPE, CONNECTION_PROVIDER,
						new TimeTestConditionModelFactory("HH:mm.ss"));

		timeConditionModel = tableConditionModel.get(DateTimeTest.TIME);
		timeConditionModel.set().equalTo(LocalTime.of(11, 00, 2));
		where = tableConditionModel.where(Conjunction.AND);
		assertEquals("(time >= ? AND time <= ?)", where.string(entityDefinition));
		assertEquals(2, where.values().size());
		assertEquals(LocalTime.of(11, 00, 2), where.values().get(0));
		assertEquals(LocalTime.of(11, 00, 2, 999_000_000), where.values().get(1));

		tableConditionModel = new DefaultEntityTableConditionModel(DateTimeTest.TYPE, CONNECTION_PROVIDER,
						new TimeTestConditionModelFactory("HH:mm.ss.SSS"));

		timeConditionModel = tableConditionModel.get(DateTimeTest.TIME);
		timeConditionModel.set().equalTo(LocalTime.of(11, 00, 3, 999_000_000));
		where = tableConditionModel.where(Conjunction.AND);
		assertEquals("time = ?", where.string(entityDefinition));
		assertEquals(1, where.values().size());
		assertEquals(LocalTime.of(11, 00, 3, 999_000_000), where.values().get(0));

		tableConditionModel = new DefaultEntityTableConditionModel(DateTimeTest.TYPE, CONNECTION_PROVIDER,
						new DateTimeTestConditionModelFactory("dd-MM-yyyy HH:mm"));

		ConditionModel<LocalDateTime> dateTimeConditionModel = tableConditionModel.get(DateTimeTest.DATE_TIME);
		dateTimeConditionModel.set().equalTo(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 45));
		where = tableConditionModel.where(Conjunction.AND);
		assertEquals("(date_time >= ? AND date_time <= ?)", where.string(entityDefinition));
		assertEquals(2, where.values().size());
		assertEquals(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 45), where.values().get(0));
		assertEquals(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 45, 59, 999_000_000), where.values().get(1));

		tableConditionModel = new DefaultEntityTableConditionModel(DateTimeTest.TYPE, CONNECTION_PROVIDER,
						new DateTimeTestConditionModelFactory("dd-MM-yyyy HH:mm.ss"));

		dateTimeConditionModel = tableConditionModel.get(DateTimeTest.DATE_TIME);
		dateTimeConditionModel.set().equalTo(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 45, 15));
		where = tableConditionModel.where(Conjunction.AND);
		assertEquals("(date_time >= ? AND date_time <= ?)", where.string(entityDefinition));
		assertEquals(2, where.values().size());
		assertEquals(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 45, 15), where.values().get(0));
		assertEquals(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 45, 15, 999_000_000), where.values().get(1));

		tableConditionModel = new DefaultEntityTableConditionModel(DateTimeTest.TYPE, CONNECTION_PROVIDER,
						new DateTimeTestConditionModelFactory("dd-MM-yyyy HH:mm.ss.SSS"));

		dateTimeConditionModel = tableConditionModel.get(DateTimeTest.DATE_TIME);
		dateTimeConditionModel.set().equalTo(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 45, 15, 999_000_000));
		where = tableConditionModel.where(Conjunction.AND);
		assertEquals("date_time = ?", where.string(entityDefinition));
		assertEquals(1, where.values().size());
		assertEquals(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 45, 15, 999_000_000), where.values().get(0));
	}

	@Test
	void dateTimeNotEqualTo() {
		EntityTableConditionModel tableConditionModel = new DefaultEntityTableConditionModel(DateTimeTest.TYPE, CONNECTION_PROVIDER,
						new TimeTestConditionModelFactory("HH:mm"));
		EntityDefinition entityDefinition = CONNECTION_PROVIDER.entities().definition(DateTimeTest.TYPE);

		ConditionModel<LocalTime> timeConditionModel = tableConditionModel.get(DateTimeTest.TIME);
		timeConditionModel.set().notEqualTo(LocalTime.of(11, 00));
		Condition where = tableConditionModel.where(Conjunction.AND);
		assertEquals("(time < ? OR time > ?)", where.string(entityDefinition));
		assertEquals(2, where.values().size());
		assertEquals(LocalTime.of(11, 00), where.values().get(0));
		assertEquals(LocalTime.of(11, 00, 59, 999_000_000), where.values().get(1));

		tableConditionModel = new DefaultEntityTableConditionModel(DateTimeTest.TYPE, CONNECTION_PROVIDER,
						new TimeTestConditionModelFactory("HH:mm.ss"));

		timeConditionModel = tableConditionModel.get(DateTimeTest.TIME);
		timeConditionModel.set().notEqualTo(LocalTime.of(11, 00, 2));
		where = tableConditionModel.where(Conjunction.AND);
		assertEquals("(time < ? OR time > ?)", where.string(entityDefinition));
		assertEquals(2, where.values().size());
		assertEquals(LocalTime.of(11, 00, 2), where.values().get(0));
		assertEquals(LocalTime.of(11, 00, 2, 999_000_000), where.values().get(1));

		tableConditionModel = new DefaultEntityTableConditionModel(DateTimeTest.TYPE, CONNECTION_PROVIDER,
						new TimeTestConditionModelFactory("HH:mm.ss.SSS"));

		timeConditionModel = tableConditionModel.get(DateTimeTest.TIME);
		timeConditionModel.set().notEqualTo(LocalTime.of(11, 00, 3, 999_000_000));
		where = tableConditionModel.where(Conjunction.AND);
		assertEquals("time <> ?", where.string(entityDefinition));
		assertEquals(1, where.values().size());
		assertEquals(LocalTime.of(11, 00, 3, 999_000_000), where.values().get(0));

		tableConditionModel = new DefaultEntityTableConditionModel(DateTimeTest.TYPE, CONNECTION_PROVIDER,
						new DateTimeTestConditionModelFactory("dd-MM-yyyy HH:mm"));

		ConditionModel<LocalDateTime> dateTimeConditionModel = tableConditionModel.get(DateTimeTest.DATE_TIME);
		dateTimeConditionModel.set().notEqualTo(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 45));
		where = tableConditionModel.where(Conjunction.AND);
		assertEquals("(date_time < ? OR date_time > ?)", where.string(entityDefinition));
		assertEquals(2, where.values().size());
		assertEquals(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 45), where.values().get(0));
		assertEquals(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 45, 59, 999_000_000), where.values().get(1));

		tableConditionModel = new DefaultEntityTableConditionModel(DateTimeTest.TYPE, CONNECTION_PROVIDER,
						new DateTimeTestConditionModelFactory("dd-MM-yyyy HH:mm.ss"));

		dateTimeConditionModel = tableConditionModel.get(DateTimeTest.DATE_TIME);
		dateTimeConditionModel.set().notEqualTo(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 45, 15));
		where = tableConditionModel.where(Conjunction.AND);
		assertEquals("(date_time < ? OR date_time > ?)", where.string(entityDefinition));
		assertEquals(2, where.values().size());
		assertEquals(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 45, 15), where.values().get(0));
		assertEquals(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 45, 15, 999_000_000), where.values().get(1));

		tableConditionModel = new DefaultEntityTableConditionModel(DateTimeTest.TYPE, CONNECTION_PROVIDER,
						new DateTimeTestConditionModelFactory("dd-MM-yyyy HH:mm.ss.SSS"));

		dateTimeConditionModel = tableConditionModel.get(DateTimeTest.DATE_TIME);
		dateTimeConditionModel.set().notEqualTo(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 45, 15, 999_000_000));
		where = tableConditionModel.where(Conjunction.AND);
		assertEquals("date_time <> ?", where.string(entityDefinition));
		assertEquals(1, where.values().size());
		assertEquals(LocalDateTime.of(1975, Month.OCTOBER, 3, 10, 45, 15, 999_000_000), where.values().get(0));
	}

	private static final class TimeTestConditionModelFactory extends EntityConditionModelFactory {

		private final String timePattern;

		public TimeTestConditionModelFactory(String timePattern) {
			super(DateTimeTest.TYPE, CONNECTION_PROVIDER);
			this.timePattern = timePattern;
		}

		@Override
		protected <T> ConditionModel<T> conditionModel(Column<T> column) {
			if (column.equals(DateTimeTest.TIME)) {
				return (ConditionModel<T>) ConditionModel.builder()
								.valueClass(LocalTime.class)
								.dateTimePattern(timePattern)
								.build();
			}

			return super.conditionModel(column);
		}
	}

	private static final class DateTimeTestConditionModelFactory extends EntityConditionModelFactory {

		private final String dateTimePattern;

		public DateTimeTestConditionModelFactory(String dateTimePattern) {
			super(DateTimeTest.TYPE, CONNECTION_PROVIDER);
			this.dateTimePattern = dateTimePattern;
		}

		@Override
		protected <T> ConditionModel<T> conditionModel(Column<T> column) {
			if (column.equals(DateTimeTest.DATE_TIME)) {
				return (ConditionModel<T>) ConditionModel.builder()
								.valueClass(LocalDateTime.class)
								.dateTimePattern(dateTimePattern)
								.build();
			}

			return super.conditionModel(column);
		}
	}
}
