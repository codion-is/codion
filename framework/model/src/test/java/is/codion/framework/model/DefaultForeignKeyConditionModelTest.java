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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.reactive.value.ValueSet;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.model.ForeignKeyConditionModel.Models;
import is.codion.framework.model.test.TestDomain;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static is.codion.common.utilities.Operator.*;
import static is.codion.framework.domain.entity.condition.Condition.all;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
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
		ForeignKeyConditionModel condition = condition().build();
		EntitySearchModel equalSearchModel = condition.models().equal().searchModel();
		EntitySearchModel inSearchModel = condition.models().in().searchModel();
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
		ForeignKeyConditionModel condition = condition().build();
		EntityComboBoxModel equalComboBoxModel = condition.models().equal().comboBoxModel();
		equalComboBoxModel.items().refresh();
		Entity sales = CONNECTION.selectSingle(Department.NAME.equalTo("SALES"));

		equalComboBoxModel.selection().item().set(sales);
		assertNull(condition.operands().equal().get());

		equalComboBoxModel.selection().item().clear();
		condition.operands().equal().set(sales);
		assertNull(equalComboBoxModel.selection().item().get());
	}

	@Test
	void operators() {
		ForeignKeyConditionModel condition = condition().build();
		assertEquals(asList(EQUAL, NOT_EQUAL, IN, NOT_IN), condition.operators());
		assertEquals(EQUAL, condition.operator().get());

		condition = condition()
						.operators(asList(IN, NOT_IN))
						.build();
		assertEquals(asList(IN, NOT_IN), condition.operators());
		assertEquals(IN, condition.operator().get());

		// the initial operator, the one clear() reverts to
		condition = condition()
						.operator(NOT_IN)
						.build();
		assertEquals(NOT_IN, condition.operator().get());
		condition.operator().set(EQUAL);
		condition.clear();
		assertEquals(NOT_IN, condition.operator().get());

		assertThrows(IllegalArgumentException.class, () -> condition().operators(emptyList()));
		assertThrows(IllegalArgumentException.class, () -> condition().operators(asList(EQUAL, LESS_THAN)));
		assertThrows(IllegalArgumentException.class, () -> condition()
						.operators(singletonList(EQUAL))
						.operator(IN)
						.build());
	}

	@Test
	void modelsCreatedOnFirstAccessOnePerOperand() {
		AtomicInteger comboBoxModels = new AtomicInteger();
		AtomicInteger searchModels = new AtomicInteger();
		ForeignKeyConditionModel condition = condition()
						.comboBoxModel(() -> {
							comboBoxModels.incrementAndGet();
							return comboBoxModel();
						})
						.searchModel(() -> {
							searchModels.incrementAndGet();
							return searchModel();
						})
						.build();
		assertEquals(0, comboBoxModels.get());
		assertEquals(0, searchModels.get());

		Models models = condition.models();
		assertSame(models.equal().comboBoxModel(), models.equal().comboBoxModel());
		assertEquals(1, comboBoxModels.get());
		assertNotSame(models.equal().comboBoxModel(), models.in().comboBoxModel());
		assertEquals(2, comboBoxModels.get());
		assertSame(models.in().searchModel(), models.in().searchModel());
		assertEquals(1, searchModels.get());
		assertNotSame(models.in().searchModel(), models.equal().searchModel());
		assertEquals(2, searchModels.get());
	}

	@Test
	void conditionAppliedToEveryModel() {
		Models models = condition().build().models();
		Supplier<Condition> accounting = () -> Department.NAME.equalTo("ACCOUNTING");
		// a model created before the condition is set
		EntityComboBoxModel equalComboBoxModel = models.equal().comboBoxModel();
		assertEquals(all(Department.TYPE), equalComboBoxModel.condition().getOrThrow().get());
		models.condition().set(accounting);
		assertSame(accounting, equalComboBoxModel.condition().get());
		// and the ones created after
		assertSame(accounting, models.in().comboBoxModel().condition().get());
		assertSame(accounting, models.equal().searchModel().condition().get());
		assertSame(accounting, models.in().searchModel().condition().get());

		Supplier<Condition> research = () -> Department.NAME.equalTo("RESEARCH");
		models.condition().set(research);
		assertSame(research, models.equal().comboBoxModel().condition().get());
		assertSame(research, models.in().comboBoxModel().condition().get());
		assertSame(research, models.equal().searchModel().condition().get());
		assertSame(research, models.in().searchModel().condition().get());

		// cleared, the models revert to their own defaults
		models.condition().clear();
		assertEquals(all(Department.TYPE), equalComboBoxModel.condition().getOrThrow().get());
		assertNotSame(research, models.equal().searchModel().condition().getOrThrow());

		// a condition set directly on a model is left alone until the shared one is set
		Supplier<Condition> sales = () -> Department.NAME.equalTo("SALES");
		models.equal().searchModel().condition().set(sales);
		assertSame(sales, models.equal().searchModel().condition().get());
		models.condition().set(accounting);
		assertSame(accounting, models.equal().searchModel().condition().get());
	}

	@Test
	void caption() {
		ForeignKeyConditionModel condition = new EntityConditions(Employee.TYPE, CONNECTION).condition(Employee.DEPARTMENT_FK);
		assertEquals(ENTITIES.definition(Employee.TYPE).foreignKeys().definition(Employee.DEPARTMENT_FK).caption(),
						condition.caption().orElseThrow());
		assertFalse(condition().build().caption().isPresent());
	}

	@Test
	void operandsCanNotShareAModel() {
		EntityComboBoxModel comboBoxModel = comboBoxModel();
		EntitySearchModel searchModel = searchModel();
		ForeignKeyConditionModel condition = condition()
						.comboBoxModel(() -> comboBoxModel)
						.searchModel(() -> searchModel)
						.build();
		Models models = condition.models();
		assertSame(comboBoxModel, models.equal().comboBoxModel());
		assertThrows(IllegalStateException.class, () -> models.in().comboBoxModel());
		assertSame(searchModel, models.in().searchModel());
		assertThrows(IllegalStateException.class, () -> models.equal().searchModel());
		// a shared model, the operators restricted to the ones of a single operand
		Models restricted = condition()
						.comboBoxModel(() -> comboBoxModel)
						.operators(asList(EQUAL, NOT_EQUAL))
						.build()
						.models();
		assertSame(comboBoxModel, restricted.equal().comboBoxModel());
	}

	@Test
	void entityConditionsBuilderInitializedWithTheDefaults() {
		EntityConditions conditions = new EntityConditions(Employee.TYPE, CONNECTION) {
			@Override
			protected ForeignKeyConditionModel condition(ForeignKey foreignKey) {
				return builder(foreignKey)
								.operators(asList(EQUAL, NOT_EQUAL))
								.build();
			}
		};
		ForeignKeyConditionModel condition = (ForeignKeyConditionModel) conditions.get().get(Employee.DEPARTMENT_FK);
		assertEquals(asList(EQUAL, NOT_EQUAL), condition.operators());
		assertEquals(ENTITIES.definition(Employee.TYPE).foreignKeys().definition(Employee.DEPARTMENT_FK).caption(),
						condition.caption().orElseThrow());
		Models models = condition.models();
		assertEquals(Department.TYPE, models.equal().comboBoxModel().entityDefinition().type());
		assertNotSame(models.equal().comboBoxModel(), models.in().comboBoxModel());
		assertEquals(Department.TYPE, models.equal().searchModel().entityDefinition().type());
		assertNotSame(models.equal().searchModel(), models.in().searchModel());
	}

	@Test
	void updatedEntitiesReplacedInOperands() {
		ForeignKeyConditionModel condition = condition().build();
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
		ForeignKeyConditionModel condition = condition().build();
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

	private static ForeignKeyConditionModel.Builder condition() {
		return ForeignKeyConditionModel.builder()
						.foreignKey(Employee.DEPARTMENT_FK)
						.connection(CONNECTION);
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
