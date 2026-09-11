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
 * Copyright (c) 2009 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.reactive.value.ValueSet;
import is.codion.common.utilities.Operator;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.test.TestDomain;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class DefaultForeignKeyConditionModelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final Entities ENTITIES = new TestDomain().entities();

	private static final EntityConnection CONNECTION = LocalEntityConnection.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	@Test
	void operandsAreNotTheSearchModelSelections() {
		EntitySearchModel equalSearchModel = searchModel();
		EntitySearchModel inSearchModel = searchModel();
		ForeignKeyConditionModel condition = ForeignKeyConditionModel.builder(Employee.DEPARTMENT_FK)
						.equalSearchModel(equalSearchModel)
						.inSearchModel(inSearchModel)
						.build();
		Entity sales = CONNECTION.selectSingle(Department.NAME.equalTo("SALES"));

		equalSearchModel.selection().entity().set(sales);
		inSearchModel.selection().entity().set(sales);
		assertNull(condition.operands().equal().get());
		assertTrue(condition.operands().in().get().isEmpty());

		equalSearchModel.selection().clear();
		inSearchModel.selection().clear();
		condition.operands().equal().set(sales);
		condition.operands().in().set(singletonList(sales));
		assertFalse(equalSearchModel.selection().present().is());
		assertFalse(inSearchModel.selection().present().is());
	}

	@Test
	void operandIsNotTheComboBoxModelSelection() {
		EntityComboBoxModel equalComboBoxModel = EntityComboBoxModel.builder()
						.entityType(Department.TYPE)
						.connection(CONNECTION)
						.build();
		equalComboBoxModel.items().refresh();
		ForeignKeyConditionModel condition = ForeignKeyConditionModel.builder(Employee.DEPARTMENT_FK)
						.equalComboBoxModel(equalComboBoxModel)
						.build();
		Entity sales = CONNECTION.selectSingle(Department.NAME.equalTo("SALES"));

		equalComboBoxModel.selection().item().set(sales);
		assertNull(condition.operands().equal().get());

		equalComboBoxModel.selection().item().clear();
		condition.operands().equal().set(sales);
		assertNull(equalComboBoxModel.selection().item().get());
	}

	@Test
	void inOperandOnly() {
		ForeignKeyConditionModel condition = ForeignKeyConditionModel.builder(Employee.DEPARTMENT_FK)
						.inSearchModel(searchModel())
						.build();
		assertEquals(asList(Operator.IN, Operator.NOT_IN), condition.operators());
		assertEquals(Operator.IN, condition.operator().get());
	}

	@Test
	void inComboBoxModel() {
		ForeignKeyConditionModel condition = ForeignKeyConditionModel.builder(Employee.DEPARTMENT_FK)
						.equalComboBoxModel(comboBoxModel())
						.inComboBoxModel(comboBoxModel())
						.build();
		assertEquals(asList(Operator.EQUAL, Operator.NOT_EQUAL, Operator.IN, Operator.NOT_IN), condition.operators());
		assertEquals(Operator.EQUAL, condition.operator().get());

		condition = ForeignKeyConditionModel.builder(Employee.DEPARTMENT_FK)
						.inComboBoxModel(comboBoxModel())
						.build();
		assertEquals(asList(Operator.IN, Operator.NOT_IN), condition.operators());
		assertEquals(Operator.IN, condition.operator().get());

		// the selection is not the operand
		EntityComboBoxModel inComboBoxModel = condition.inComboBoxModel().orElseThrow();
		inComboBoxModel.items().refresh();
		Entity sales = CONNECTION.selectSingle(Department.NAME.equalTo("SALES"));
		inComboBoxModel.selection().item().set(sales);
		assertTrue(condition.operands().in().get().isEmpty());
		inComboBoxModel.selection().item().clear();
		condition.operands().in().set(singletonList(sales));
		assertNull(inComboBoxModel.selection().item().get());

		assertThrows(IllegalStateException.class, () -> ForeignKeyConditionModel.builder(Employee.DEPARTMENT_FK)
						.inSearchModel(searchModel())
						.inComboBoxModel(comboBoxModel())
						.build());
	}

	@Test
	void caption() {
		ForeignKeyConditionModel condition = new EntityConditions(Employee.TYPE, CONNECTION).condition(Employee.DEPARTMENT_FK);
		assertEquals(ENTITIES.definition(Employee.TYPE).foreignKeys().definition(Employee.DEPARTMENT_FK).caption(),
						condition.caption().orElseThrow());
		assertFalse(ForeignKeyConditionModel.builder(Employee.DEPARTMENT_FK)
						.inSearchModel(searchModel())
						.build()
						.caption()
						.isPresent());
	}

	@Test
	void updatedEntitiesReplacedInOperands() {
		ForeignKeyConditionModel condition = ForeignKeyConditionModel.builder(Employee.DEPARTMENT_FK)
						.equalSearchModel(searchModel())
						.inSearchModel(searchModel())
						.build();
		Entity renamed = department(-42, "Renamed");
		Entity rekeyed = department(-43, "Rekeyed");
		Entity untouched = department(-44, "Untouched");
		condition.operands().equal().set(rekeyed);
		condition.operands().in().set(asList(renamed, rekeyed, untouched));
		AtomicInteger inNotifications = new AtomicInteger();
		condition.operands().in().addListener(inNotifications::incrementAndGet);

		Entity renamedBefore = renamed.copy().mutable();
		renamedBefore.set(Department.NAME, "New name");
		Entity renamedAfter = renamedBefore.copy().mutable();
		renamedAfter.save();
		// the primary key modified, matched on its original value
		Entity rekeyedBefore = rekeyed.copy().mutable();
		rekeyedBefore.set(Department.ID, -45);
		Entity rekeyedAfter = rekeyedBefore.copy().mutable();
		rekeyedAfter.save();

		Map<Entity, Entity> updated = new HashMap<>();
		updated.put(renamedBefore, renamedAfter);
		updated.put(rekeyedBefore, rekeyedAfter);
		PersistenceEvents.persistenceEvents(Department.TYPE).updated().accept(updated);

		assertSame(rekeyedAfter, condition.operands().equal().get());
		ValueSet<Entity> in = condition.operands().in();
		assertEquals(3, in.size());
		assertTrue(in.get().stream().anyMatch(entity -> entity == renamedAfter));
		assertTrue(in.get().stream().anyMatch(entity -> entity == rekeyedAfter));
		assertTrue(in.get().stream().anyMatch(entity -> entity == untouched));
		// a single set(), no intermediate state
		assertEquals(1, inNotifications.get());

		// an update not concerning the operands leaves them alone
		Entity other = department(-46, "Other");
		Entity otherBefore = other.copy().mutable();
		otherBefore.set(Department.NAME, "Changed");
		Entity otherAfter = otherBefore.copy().mutable();
		otherAfter.save();
		Map<Entity, Entity> otherUpdated = new HashMap<>();
		otherUpdated.put(otherBefore, otherAfter);
		PersistenceEvents.persistenceEvents(Department.TYPE).updated().accept(otherUpdated);
		assertEquals(1, inNotifications.get());
		assertSame(rekeyedAfter, condition.operands().equal().get());
	}

	@Test
	void deletedEntitiesRemovedFromOperands() {
		ForeignKeyConditionModel condition = ForeignKeyConditionModel.builder(Employee.DEPARTMENT_FK)
						.equalSearchModel(searchModel())
						.inSearchModel(searchModel())
						.build();
		Entity one = department(-42, "One");
		Entity two = department(-43, "Two");
		condition.operands().equal().set(one);
		condition.operands().in().set(asList(one, two));
		AtomicInteger inNotifications = new AtomicInteger();
		condition.operands().in().addListener(inNotifications::incrementAndGet);

		// a delete not concerning the operands leaves them alone
		PersistenceEvents.persistenceEvents(Department.TYPE).deleted().accept(singletonList(department(-44, "Other")));
		assertEquals(0, inNotifications.get());
		assertSame(one, condition.operands().equal().get());

		PersistenceEvents.persistenceEvents(Department.TYPE).deleted().accept(singletonList(one));
		assertNull(condition.operands().equal().get());
		assertEquals(singletonList(two), asList(condition.operands().in().get().toArray()));
		assertEquals(1, inNotifications.get());
	}

	private static EntitySearchModel searchModel() {
		return EntitySearchModel.builder()
						.entityType(Department.TYPE)
						.connection(CONNECTION)
						.build();
	}

	private static EntityComboBoxModel comboBoxModel() {
		return EntityComboBoxModel.builder()
						.entityType(Department.TYPE)
						.connection(CONNECTION)
						.build();
	}

	private static Entity department(int id, String name) {
		return ENTITIES.entity(Department.TYPE)
						.with(Department.ID, id)
						.with(Department.NAME, name)
						.build();
	}
}
