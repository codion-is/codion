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

import is.codion.common.Conjunction;
import is.codion.common.Operator;
import is.codion.common.model.condition.ConditionModel;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.test.TestDomain;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Detail;
import is.codion.framework.model.test.TestDomain.Employee;

import org.junit.jupiter.api.Test;

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
}
