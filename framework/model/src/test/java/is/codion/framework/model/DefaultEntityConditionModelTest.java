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
 * Copyright (c) 2016 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultEntityConditionModelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	private final EntityConditionModel conditionModel = new DefaultEntityConditionModel(Employee.TYPE,
					CONNECTION_PROVIDER, new AttributeConditionModelFactory(CONNECTION_PROVIDER));

	@Test
	void test() {
		assertEquals(Employee.TYPE, conditionModel.entityType());
		assertEquals(10, conditionModel.get().size());

		assertFalse(conditionModel.get(Employee.DEPARTMENT_FK).enabled().get());

		assertFalse(conditionModel.enabled().get());
		conditionModel.get(Employee.DEPARTMENT_FK).enabled().set(true);
		assertTrue(conditionModel.enabled().get());
	}

	@Test
	void noSearchColumnsDefined() {
		EntityConditionModel model = new DefaultEntityConditionModel(Detail.TYPE,
						CONNECTION_PROVIDER, new AttributeConditionModelFactory(CONNECTION_PROVIDER));
		//no search columns defined for master entity
		ConditionModel<Entity> masterModel =
						model.attribute(Detail.MASTER_FK);
		assertThrows(IllegalStateException.class, () ->
						((ForeignKeyConditionModel) masterModel).equalSearchModel().search());
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
		assertFalse(conditionModel.get(Employee.DEPARTMENT_FK).enabled().get());
		boolean searchStateChanged = conditionModel.setEqualOperand(Employee.DEPARTMENT_FK, sales);
		assertTrue(searchStateChanged);
		assertTrue(conditionModel.get(Employee.DEPARTMENT_FK).enabled().get());
		ConditionModel<Entity> deptModel =
						conditionModel.attribute(Employee.DEPARTMENT_FK);
		assertSame(deptModel.operands().equal().get(), sales);
		assertThrows(NullPointerException.class, () -> conditionModel.setEqualOperand(null, sales));
		searchStateChanged = conditionModel.setEqualOperand(Employee.DEPARTMENT_FK, null);
		assertTrue(searchStateChanged);
		assertFalse(conditionModel.get(Employee.DEPARTMENT_FK).enabled().get());
	}

	@Test
	void setInOperands() {
		Entity sales = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME.equalTo("SALES"));
		Entity accounting = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME.equalTo("ACCOUNTING"));
		assertFalse(conditionModel.get(Employee.DEPARTMENT_FK).enabled().get());
		boolean searchStateChanged = conditionModel.setInOperands(Employee.DEPARTMENT_FK, asList(sales, accounting));
		assertTrue(searchStateChanged);
		assertTrue(conditionModel.get(Employee.DEPARTMENT_FK).enabled().get());
		ConditionModel<Entity> deptModel =
						conditionModel.attribute(Employee.DEPARTMENT_FK);
		assertTrue(deptModel.operands().in().get().contains(sales));
		assertTrue(deptModel.operands().in().get().contains(accounting));
		assertThrows(NullPointerException.class, () -> conditionModel.setInOperands(Employee.DEPARTMENT_FK, null));
		assertThrows(NullPointerException.class, () -> conditionModel.setInOperands(null, emptyList()));
		searchStateChanged = conditionModel.setInOperands(Employee.DEPARTMENT_FK, emptyList());
		assertTrue(searchStateChanged);
		assertFalse(conditionModel.get(Employee.DEPARTMENT_FK).enabled().get());
	}

	@Test
	void clearConditions() {
		Entity sales = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME.equalTo("SALES"));
		Entity accounting = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME.equalTo("ACCOUNTING"));
		assertFalse(conditionModel.get(Employee.DEPARTMENT_FK).enabled().get());
		conditionModel.setInOperands(Employee.DEPARTMENT_FK, asList(sales, accounting));
		assertTrue(conditionModel.get(Employee.DEPARTMENT_FK).enabled().get());
		conditionModel.clear();
		assertFalse(conditionModel.get(Employee.DEPARTMENT_FK).enabled().get());
	}
}
