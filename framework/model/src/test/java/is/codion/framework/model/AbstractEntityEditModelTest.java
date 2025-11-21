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
 * Copyright (c) 2009 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.db.exception.UpdateException;
import is.codion.common.model.CancelException;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.EntityValidator;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ValueAttributeDefinition;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.model.EntityEditModel.EntityEditor;
import is.codion.framework.model.test.TestDomain;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Derived;
import is.codion.framework.model.test.TestDomain.Detail;
import is.codion.framework.model.test.TestDomain.Employee;
import is.codion.framework.model.test.TestDomain.NonGeneratedPK;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

public final class AbstractEntityEditModelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final Entities ENTITIES = new TestDomain().entities();

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	private AbstractEntityEditModel employeeEditModel;
	private EntityEditor editor;

	@BeforeEach
	void setUp() {
		employeeEditModel = new TestEntityEditModel(Employee.TYPE, CONNECTION_PROVIDER);
		editor = employeeEditModel.editor();
		editor.value(Employee.HIREDATE).defaultValue().set(LocalDate::now);
		Entity jones = CONNECTION_PROVIDER.connection().selectSingle(Employee.ID.equalTo(3));//JONES, used in containsUnsavedData()
		editor.value(Employee.MGR_FK).defaultValue().set(() -> jones);
	}

	@Test
	void editEvents() {
		AtomicInteger insertEvents = new AtomicInteger();
		AtomicInteger updateEvents = new AtomicInteger();
		AtomicInteger deleteEvents = new AtomicInteger();

		Consumer<Collection<Entity>> insertListener = inserted -> insertEvents.incrementAndGet();
		Consumer<Map<Entity, Entity>> updateListener = updated -> updateEvents.incrementAndGet();
		Consumer<Collection<Entity>> deleteListener = deleted -> deleteEvents.incrementAndGet();

		AbstractEntityEditModel.editEvents(Employee.TYPE).inserted().addWeakConsumer(insertListener);
		AbstractEntityEditModel.editEvents(Employee.TYPE).updated().addWeakConsumer(updateListener);
		AbstractEntityEditModel.editEvents(Employee.TYPE).deleted().addWeakConsumer(deleteListener);

		employeeEditModel.settings().editEvents().set(true);

		EntityConnection connection = employeeEditModel.connection();
		connection.startTransaction();
		try {
			Entity jones = connection.selectSingle(Employee.NAME.equalTo("JONES"));
			editor.set(jones);
			editor.value(Employee.NAME).set("Noname");
			employeeEditModel.insert();
			assertEquals(1, insertEvents.get());
			editor.value(Employee.NAME).set("Another");
			employeeEditModel.update();
			assertEquals(1, updateEvents.get());
			assertNotNull(employeeEditModel.delete());
			assertEquals(1, deleteEvents.get());
		}
		finally {
			connection.rollbackTransaction();
		}

		AbstractEntityEditModel.editEvents(Employee.TYPE).inserted().removeWeakConsumer(insertListener);
		AbstractEntityEditModel.editEvents(Employee.TYPE).updated().removeWeakConsumer(updateListener);
		AbstractEntityEditModel.editEvents(Employee.TYPE).deleted().removeWeakConsumer(deleteListener);
	}

	@Test
	void searchModel() {
		EntitySearchModel model = employeeEditModel.searchModel(Employee.DEPARTMENT_FK);
		assertNotNull(model);
		assertSame(model, employeeEditModel.searchModel(Employee.DEPARTMENT_FK));
	}

	@Test
	void createSearchModel() {
		EntitySearchModel model = employeeEditModel.createSearchModel(Employee.DEPARTMENT_FK);
		assertNotNull(model);
		assertEquals(Department.TYPE, model.entityDefinition().type());
	}

	@Test
	void refresh() {
		EntityConnection connection = employeeEditModel.connection();
		connection.startTransaction();
		try {
			Entity employee = connection.selectSingle(Employee.NAME.equalTo("MARTIN"));
			employeeEditModel.refresh();
			editor.set(employee);
			employee.set(Employee.NAME, "NOONE");
			connection.update(employee);
			employeeEditModel.refresh();
			assertEquals("NOONE", editor.value(Employee.NAME).get());

			EntityEditModel departmentEditModel = new TestEntityEditModel(Department.TYPE, employeeEditModel.connectionProvider());
			Entity accounting = connection.selectSingle(Department.NAME.equalTo("ACCOUNTING"));
			EntityEditor departmentEditor = departmentEditModel.editor();
			departmentEditor.set(accounting);
			departmentEditor.value(Department.ID).set(-20);

			accounting.set(Department.NAME, "Accounting");
			connection.update(accounting);
			departmentEditModel.refresh();
			assertEquals(10, departmentEditor.value(Department.ID).get());
			assertEquals("Accounting", departmentEditor.value(Department.NAME).get());
		}
		finally {
			connection.rollbackTransaction();
		}
	}

	@Test
	void entityCopy() {
		Entity employee = employeeEditModel.connection().selectSingle(
						Employee.NAME.equalTo("MARTIN"));
		editor.set(employee);
		Entity copyWithPrimaryKeyValue = editor.get();
		assertEquals(employee, copyWithPrimaryKeyValue);
		assertFalse(copyWithPrimaryKeyValue.primaryKey().isNull());
	}

	@Test
	void constructorValidation() {
		assertThrows(NullPointerException.class, () -> new TestEntityEditModel(null, CONNECTION_PROVIDER));
		assertThrows(NullPointerException.class, () -> new TestEntityEditModel(Employee.TYPE, null));
	}

	@Test
	void defaultForeignKeyValue() {
		Entity employee = employeeEditModel.connection().selectSingle(
						Employee.NAME.equalTo("MARTIN"));
		editor.set(employee);
		//clear the department foreign key value
		Entity dept = editor.get().entity(Employee.DEPARTMENT_FK);
		editor.value(Employee.DEPARTMENT_FK).clear();
		//set the reference key attribute value
		assertFalse(editor.value(Employee.DEPARTMENT_FK).present().is());
		editor.value(Employee.DEPARTMENT).set(dept.get(Department.ID));
		assertNull(editor.get().get(Employee.DEPARTMENT_FK));
		dept = editor.value(Employee.DEPARTMENT_FK).get();
		assertNull(dept);
		editor.defaults();
		assertNotNull(editor.value(Employee.DEPARTMENT_FK).get());
	}

	@Test
	void defaults() {
		editor.value(Employee.NAME).defaultValue().set(() -> "Scott");
		assertFalse(editor.value(Employee.NAME).present().is());
		editor.defaults();
		assertEquals("Scott", editor.value(Employee.NAME).get());

		editor.value(Employee.NAME).defaultValue().set(() -> null);
		editor.defaults();
		assertFalse(editor.value(Employee.NAME).present().is());
	}

	@Test
	void clear() {
		Entity employee = employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("MARTIN"));
		editor.set(employee);
		editor.defaults();
		assertTrue(editor.value(Employee.DEPARTMENT_FK).present().is());//persists
		assertFalse(editor.value(Employee.NAME).present().is());
		editor.set(employee);
		editor.clear();
		assertFalse(editor.value(Employee.DEPARTMENT_FK).present().is());//should not persist on clear
		assertFalse(editor.value(Employee.NAME).present().is());
	}

	@Test
	void test() {
		ObservableState primaryKeyNullState = editor.primaryKeyNull();
		ObservableState entityExistsState = editor.exists();

		assertTrue(primaryKeyNullState.is());
		assertFalse(entityExistsState.is());

		Consumer<Object> consumer = data -> {};
		employeeEditModel.afterDelete().addConsumer(consumer);
		employeeEditModel.afterInsert().addConsumer(consumer);
		employeeEditModel.afterUpdate().addConsumer(consumer);
		employeeEditModel.beforeDelete().addConsumer(consumer);
		employeeEditModel.beforeInsert().addConsumer(consumer);
		employeeEditModel.beforeUpdate().addConsumer(consumer);
		Runnable listener = () -> {};
		employeeEditModel.afterInsertUpdateOrDelete().addListener(listener);

		assertEquals(Employee.TYPE, employeeEditModel.entityType());

		assertFalse(editor.exists().is());
		assertFalse(editor.modified().is());

		Entity employee = employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("MARTIN"));
		editor.set(employee);
		assertFalse(primaryKeyNullState.is());
		assertTrue(entityExistsState.is());

		assertTrue(editor.get().equalValues(employee), "Active entity is not equal to the entity just set");
		assertTrue(editor.exists().is(), "Active entity exists after an entity is set");
		assertFalse(editor.modified().is());
		editor.defaults();
		assertFalse(editor.exists().is(), "Active entity exists after defaults are set");
		assertFalse(editor.modified().is());
		assertTrue(editor.get().primaryKey().isNull(), "Active entity primary key is not null after defaults are set");

		editor.set(employee);
		assertFalse(editor.get().primaryKey().isNull(), "Active entity primary key is null after entity is set");

		Integer originalEmployeeId = editor.value(Employee.ID).get();
		editor.value(Employee.ID).clear();
		assertTrue(primaryKeyNullState.is());
		editor.value(Employee.ID).set(originalEmployeeId);
		assertFalse(primaryKeyNullState.is());

		editor.defaults();
		assertFalse(entityExistsState.is());

		Double originalCommission = editor.value(Employee.COMMISSION).get();
		double commission = 1500.5;
		LocalDate originalHiredate = editor.value(Employee.HIREDATE).get();
		LocalDate hiredate = LocalDate.now();
		String originalName = editor.value(Employee.NAME).get();
		String name = "Mr. Mr";

		editor.value(Employee.COMMISSION).set(commission);
		editor.value(Employee.HIREDATE).set(hiredate);
		editor.value(Employee.NAME).set(name);

		assertEquals(commission, editor.value(Employee.COMMISSION).get(), "Commission does not fit");
		assertEquals(hiredate, editor.value(Employee.HIREDATE).get(), "Hiredate does not fit");
		assertEquals(name, editor.value(Employee.NAME).get(), "Name does not fit");

		editor.value(Employee.COMMISSION).set(originalCommission);
		editor.value(Employee.HIREDATE).set(originalHiredate);
		editor.value(Employee.NAME).set(originalName);

		editor.value(Employee.COMMISSION).set(50d);
		editor.value(Employee.COMMISSION).clear();
		assertNull(editor.value(Employee.COMMISSION).get());

		//test validation
		try {
			editor.value(Employee.COMMISSION).set(50d);
			editor.validate(Employee.COMMISSION);
			fail("Validation should fail on invalid commission value");
		}
		catch (ValidationException e) {
			assertEquals(Employee.COMMISSION, e.attribute());
			assertEquals(50d, e.value());
			ValueAttributeDefinition<?> attributeDefinition = (ValueAttributeDefinition<?>)
							ENTITIES.definition(Employee.TYPE).attributes().definition(e.attribute());
			assertTrue(e.getMessage().contains(attributeDefinition.toString()));
			assertTrue(e.getMessage().contains(attributeDefinition.minimum().map(Objects::toString).get()));
		}

		editor.defaults();
		assertTrue(editor.get().primaryKey().isNull(), "Active entity is not null after model is cleared");

		employeeEditModel.afterDelete().removeConsumer(consumer);
		employeeEditModel.afterInsert().removeConsumer(consumer);
		employeeEditModel.afterUpdate().removeConsumer(consumer);
		employeeEditModel.beforeDelete().removeConsumer(consumer);
		employeeEditModel.beforeInsert().removeConsumer(consumer);
		employeeEditModel.beforeUpdate().removeConsumer(consumer);
		employeeEditModel.afterInsertUpdateOrDelete().removeListener(listener);
	}

	@Test
	void insertReadOnly() throws CancelException {
		employeeEditModel.settings().readOnly().set(true);
		assertThrows(IllegalStateException.class, () -> employeeEditModel.insert());
	}

	@Test
	void updateReadOnly() throws CancelException {
		employeeEditModel.settings().readOnly().set(true);
		assertThrows(IllegalStateException.class, () -> employeeEditModel.update());
	}

	@Test
	void deleteReadOnly() throws CancelException {
		employeeEditModel.settings().readOnly().set(true);
		assertThrows(IllegalStateException.class, () -> employeeEditModel.delete());
	}

	@Test
	void insert() {
		assertTrue(employeeEditModel.insert(emptyList()).isEmpty());
		EntityConnection connection = employeeEditModel.connection();
		connection.startTransaction();
		try {
			editor.value(Employee.COMMISSION).set(1000d);
			editor.value(Employee.HIREDATE).set(LocalDate.now());
			editor.value(Employee.JOB).set("CLERK");
			editor.value(Employee.NAME).set("Björn");
			editor.value(Employee.SALARY).set(1000d);

			Entity tmpDept = ENTITIES.entity(Department.TYPE)
							.with(Department.ID, 99)
							.with(Department.LOCATION, "Limbo")
							.with(Department.NAME, "Judgment")
							.build();

			Entity department = connection.insertSelect(tmpDept);

			editor.value(Employee.DEPARTMENT_FK).set(department);

			employeeEditModel.afterInsert().addConsumer(insertedEntities ->
							assertEquals(department, insertedEntities.iterator().next().get(Employee.DEPARTMENT_FK)));
			employeeEditModel.settings().insertEnabled().set(false);
			assertFalse(employeeEditModel.settings().insertEnabled().is());
			assertThrows(IllegalStateException.class, () -> employeeEditModel.insert());
			employeeEditModel.settings().insertEnabled().set(true);
			assertTrue(employeeEditModel.settings().insertEnabled().is());

			employeeEditModel.insert();
			assertTrue(editor.exists().is());
			Entity entityCopy = editor.get();
			assertFalse(entityCopy.primaryKey().isNull());
			assertEquals(entityCopy.primaryKey(), entityCopy.originalPrimaryKey());

			editor.value(Employee.NAME).set("Bobby");
			try {
				employeeEditModel.insert();
			}
			catch (Exception e) {
				fail("Should be able to insert again");
			}
		}
		finally {
			connection.rollbackTransaction();
		}
	}

	@Test
	void update() {
		assertTrue(employeeEditModel.update(emptyList()).isEmpty());
		EntityConnection connection = employeeEditModel.connection();
		connection.startTransaction();
		try {
			editor.set(connection.selectSingle(Employee.NAME.equalTo("MILLER")));
			assertFalse(editor.modified().is());
			editor.value(Employee.NAME).set("BJORN");
			assertTrue(editor.modified().is());
			List<Entity> toUpdate = singletonList(editor.get());
			Consumer<Map<Entity, Entity>> consumer = updatedEntities ->
							assertEquals(toUpdate, new ArrayList<>(updatedEntities.values()));
			employeeEditModel.afterUpdate().addConsumer(consumer);
			employeeEditModel.settings().updateEnabled().set(false);
			assertFalse(employeeEditModel.settings().updateEnabled().is());
			assertThrows(IllegalStateException.class, () -> employeeEditModel.update());
			employeeEditModel.settings().updateEnabled().set(true);
			assertTrue(employeeEditModel.settings().updateEnabled().is());

			employeeEditModel.update();
			assertFalse(editor.modified().is());
			employeeEditModel.afterUpdate().removeConsumer(consumer);

			employeeEditModel.settings().updateMultipleEnabled().set(false);

			Entity emp1 = connection.selectSingle(Employee.NAME.equalTo("BLAKE"));
			emp1.set(Employee.COMMISSION, 100d);
			Entity emp2 = connection.selectSingle(Employee.NAME.equalTo("JONES"));
			emp2.set(Employee.COMMISSION, 100d);
			assertThrows(IllegalStateException.class, () -> employeeEditModel.update(Arrays.asList(emp1, emp2)));

			// Test afterUpdate event map contents
			EntityEditModel deptEditModel = new TestEntityEditModel(Department.TYPE, CONNECTION_PROVIDER);
			deptEditModel.editor().value(Department.ID).set(-1);
			deptEditModel.editor().value(Department.NAME).set("UpdTest");
			Entity dept = deptEditModel.insert();
			deptEditModel.editor().set(dept);

			AtomicBoolean checker = new AtomicBoolean(false);

			deptEditModel.afterUpdate().addConsumer(updated -> {
				checker.set(true);
				Map.Entry<Entity, Entity> entry = updated.entrySet().iterator().next();
				Entity beforeUpdate = entry.getKey();
				Entity afterUpdate = entry.getValue();
				assertEquals(Integer.valueOf(-1), beforeUpdate.originalPrimaryKey().value());
				assertEquals("UpdTest", beforeUpdate.original(Department.NAME));
				assertEquals(Integer.valueOf(-2), beforeUpdate.primaryKey().value());
				assertEquals("UpdTest2", beforeUpdate.get(Department.NAME));
				assertEquals(Integer.valueOf(-2), afterUpdate.primaryKey().value());
				assertEquals("UpdTest2", afterUpdate.get(Department.NAME));
			});

			deptEditModel.editor().value(Department.ID).set(-2);
			deptEditModel.editor().value(Department.NAME).set("UpdTest2");
			deptEditModel.update();

			assertTrue(checker.get(), "After update event should have been triggered");
		}
		finally {
			connection.rollbackTransaction();
			employeeEditModel.settings().updateMultipleEnabled().set(true);
		}
	}

	@Test
	void delete() {
		assertTrue(employeeEditModel.delete(emptyList()).isEmpty());
		EntityConnection connection = employeeEditModel.connection();
		connection.startTransaction();
		try {
			editor.set(connection.selectSingle(Employee.NAME.equalTo("MILLER")));
			List<Entity> toDelete = singletonList(editor.get());
			employeeEditModel.afterDelete().addConsumer(deletedEntities -> assertTrue(toDelete.containsAll(deletedEntities)));
			employeeEditModel.settings().deleteEnabled().set(false);
			assertFalse(employeeEditModel.settings().deleteEnabled().is());
			assertThrows(IllegalStateException.class, () -> employeeEditModel.delete());
			employeeEditModel.settings().deleteEnabled().set(true);
			assertTrue(employeeEditModel.settings().deleteEnabled().is());

			employeeEditModel.delete();
		}
		finally {
			connection.rollbackTransaction();
		}
	}

	@Test
	void setEntity() {
		Entity martin = employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("MARTIN"));
		Entity king = employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("KING"));
		editor.set(king);
		editor.value(Employee.MGR_FK).set(martin);
		editor.defaults();
		king.set(Employee.MGR_FK, null);
		editor.set(king);
		assertNull(editor.value(Employee.MGR_FK).get());
		editor.defaults();
		assertEquals(LocalDate.now(), editor.value(Employee.HIREDATE).get());
		assertFalse(editor.get().modified(Employee.HIREDATE));
		assertFalse(editor.get().modified());
	}

	@Test
	void modifiedAttributes() {
		Entity martin = employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("MARTIN"));
		Entity king = employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("KING"));
		editor.set(martin);

		editor.value(Employee.MGR_FK).clear();
		assertTrue(editor.modified().attributes().containsOnly(singleton(Employee.MGR_FK)));
		editor.value(Employee.MGR_FK).revert();
		assertTrue(editor.modified().attributes().isEmpty());

		editor.value(Employee.NAME).set("NewName");
		assertTrue(editor.modified().attributes().containsOnly(singleton(Employee.NAME)));
		editor.value(Employee.MGR_FK).set(king);
		assertTrue(editor.modified().attributes().containsOnly(Arrays.asList(Employee.MGR_FK, Employee.NAME)));
		editor.value(Employee.SALARY).set(3210d);
		assertTrue(editor.modified().attributes().containsOnly(Arrays.asList(Employee.SALARY, Employee.NAME, Employee.MGR_FK)));
	}

	@Test
	void persist() {
		Entity king = employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("KING"));
		editor.set(king);
		assertNotNull(editor.value(Employee.JOB).get());
		editor.value(Employee.JOB).persist().set(true);
		editor.defaults();
		assertNotNull(editor.value(Employee.JOB).get());
		editor.set(king);
		editor.value(Employee.JOB).persist().set(false);
		editor.defaults();
		assertNull(editor.value(Employee.JOB).get());
		assertThrows(IllegalArgumentException.class, () -> editor.value(Department.ID).persist().set(true));
		assertThrows(IllegalArgumentException.class, () -> editor.value(Department.ID).persist().is());
	}

	@Test
	void updated() {
		Entity james = employeeEditModel.connection()
						.selectSingle(Employee.NAME.equalTo("JAMES"));
		editor.set(james);
		assertTrue(editor.get().entity(Employee.MGR_FK).isNull(Employee.COMMISSION));
		Entity blake = employeeEditModel.connection()
						.selectSingle(Employee.NAME.equalTo("BLAKE"));
		blake.set(Employee.COMMISSION, 100d);
		employeeEditModel.updated(Employee.MGR_FK, singletonMap(blake.primaryKey(), blake));
		assertEquals(100d, editor.get().entity(Employee.MGR_FK).get(Employee.COMMISSION));
	}

	@Test
	void value() {
		Value<Integer> value = editor.value(Employee.MGR);
		assertSame(value, editor.value(Employee.MGR));
		value.set(42);
		assertEquals(42, editor.value(Employee.MGR).get());
		editor.value(Employee.MGR).set(2);
		assertEquals(2, value.get());
		value.clear();
		assertFalse(editor.value(Employee.MGR).optional().isPresent());
		assertFalse(editor.value(Employee.MGR_FK).optional().isPresent());
		value.set(3);
		assertTrue(editor.value(Employee.MGR).optional().isPresent());

		assertThrows(IllegalArgumentException.class, () -> editor.value(Department.ID));
		assertThrows(IllegalArgumentException.class, () -> editor.value(Department.ID));
	}

	@Test
	void derivedAttributes() {
		EntityEditModel editModel = new DetailEditModel(employeeEditModel.connectionProvider());

		AtomicInteger derivedCounter = new AtomicInteger();
		AtomicInteger derivedEditCounter = new AtomicInteger();

		editModel.editor().value(Detail.INT_DERIVED).addConsumer(value -> derivedCounter.incrementAndGet());
		editModel.editor().value(Detail.INT_DERIVED).edited().addConsumer(value -> derivedEditCounter.incrementAndGet());

		editModel.editor().value(Detail.INT).set(1);
		assertEquals(1, derivedCounter.get());
		assertEquals(1, derivedEditCounter.get());

		editModel.editor().value(Detail.INT).set(2);
		assertEquals(2, derivedCounter.get());
		assertEquals(2, derivedEditCounter.get());

		Entity detail = ENTITIES.entity(Detail.TYPE)
						.with(Detail.INT, 3)
						.build();
		editModel.editor().set(detail);
		assertEquals(3, derivedCounter.get());
		assertEquals(2, derivedEditCounter.get());
	}

	@Test
	void persistWritableForeignKey() {
		EntityEditModel editModel = new DetailEditModel(employeeEditModel.connectionProvider());
		assertFalse(editModel.editor().value(Detail.MASTER_FK).persist().is());//not writable
	}

	@Test
	void foreignKeys() {
		AtomicInteger deptNoChange = new AtomicInteger();
		editor.value(Employee.DEPARTMENT).addConsumer(value -> deptNoChange.incrementAndGet());
		AtomicInteger deptChange = new AtomicInteger();
		editor.value(Employee.DEPARTMENT_FK).addConsumer(value -> deptChange.incrementAndGet());
		AtomicInteger deptEdit = new AtomicInteger();
		editor.value(Employee.DEPARTMENT_FK).edited().addConsumer(value -> deptEdit.incrementAndGet());

		Entity dept = employeeEditModel.connection().selectSingle(Department.ID.equalTo(10));
		editor.value(Employee.DEPARTMENT_FK).set(dept);

		editor.value(Employee.DEPARTMENT).set(20);
		assertEquals(2, deptNoChange.get());
		assertEquals(2, deptChange.get());
		assertEquals(2, deptEdit.get());

		dept = employeeEditModel.connection().selectSingle(Department.ID.equalTo(20));
		editor.value(Employee.DEPARTMENT_FK).set(dept);

		assertEquals(2, deptNoChange.get());
		assertEquals(3, deptChange.get());
		assertEquals(3, deptEdit.get());

		editor.value(Employee.DEPARTMENT).set(30);

		assertEquals(3, deptNoChange.get());
		assertEquals(4, deptChange.get());
		assertEquals(4, deptEdit.get());

		dept = employeeEditModel.connection().selectSingle(Department.ID.equalTo(30));
		editor.value(Employee.DEPARTMENT_FK).set(dept);

		assertEquals(3, deptNoChange.get());
		assertEquals(5, deptChange.get());
		assertEquals(5, deptEdit.get());
	}

	@Test
	void modifiedAndNullObserver() {
		editor.value(Employee.NAME).set("NAME");
		//only modified when the entity is not new
		assertFalse(editor.value(Employee.NAME).modified().is());
		Entity martin = employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("MARTIN"));
		editor.set(martin);
		State modifiedState = State.state();
		State nullState = State.state();
		ObservableState nameModifiedObserver = editor.value(Employee.NAME).modified();
		nameModifiedObserver.addConsumer(modifiedState::set);
		ObservableState nameIsNull = editor.value(Employee.NAME).present().not();
		nameIsNull.addConsumer(nullState::set);

		editor.value(Employee.NAME).set("JOHN");
		assertTrue(nameModifiedObserver.is());
		assertTrue(modifiedState.is());
		editor.value(Employee.NAME).clear();
		assertTrue(nameModifiedObserver.is());
		assertTrue(modifiedState.is());
		assertTrue(nameIsNull.is());
		assertTrue(nullState.is());
		assertTrue(nullState.is());
		editor.set(martin);
		assertFalse(nameModifiedObserver.is());
		assertFalse(modifiedState.is());
		assertFalse(nameIsNull.is());
		assertFalse(nullState.is());
	}

	@Test
	void modifiedUpdate() {
		EntityConnection connection = employeeEditModel.connection();
		ObservableState nameModifiedObserver = editor.value(Employee.NAME).modified();
		Entity martin = connection.selectSingle(Employee.NAME.equalTo("MARTIN"));
		editor.set(martin);
		editor.value(Employee.NAME).set("MARTINEZ");
		assertTrue(nameModifiedObserver.is());
		connection.startTransaction();
		try {
			employeeEditModel.update();
			assertFalse(nameModifiedObserver.is());
		}
		finally {
			connection.rollbackTransaction();
		}
	}

	@Test
	public void derivedValues() {
		EntityEditModel editModel = new TestEntityEditModel(Derived.TYPE, CONNECTION_PROVIDER);

		List<Attribute<?>> changed = new ArrayList<>();
		editModel.editor().value(Derived.INT1).addListener(() -> changed.add(Derived.INT1));
		editModel.editor().value(Derived.INT2).addListener(() -> changed.add(Derived.INT2));
		editModel.editor().value(Derived.INT3).addListener(() -> changed.add(Derived.INT3));
		editModel.editor().value(Derived.INT4).addListener(() -> changed.add(Derived.INT4));

		List<Attribute<?>> edited = new ArrayList<>();
		editModel.editor().value(Derived.INT1).edited().addListener(() -> edited.add(Derived.INT1));
		editModel.editor().value(Derived.INT2).edited().addListener(() -> edited.add(Derived.INT2));
		editModel.editor().value(Derived.INT3).edited().addListener(() -> edited.add(Derived.INT3));
		editModel.editor().value(Derived.INT4).edited().addListener(() -> edited.add(Derived.INT4));

		// INT2 is derived from INT1, INT3 from INT2 etc.,
		// each adding one to the value it is derived from
		editModel.editor().value(Derived.INT1).set(1);
		assertEquals(1, editModel.editor().value(Derived.INT1).get());
		assertEquals(2, editModel.editor().value(Derived.INT2).get());
		assertEquals(3, editModel.editor().value(Derived.INT3).get());
		assertEquals(4, editModel.editor().value(Derived.INT4).get());

		// Assert the order in which change/edit events are received
		List<Column<Integer>> expected = Arrays.asList(Derived.INT1, Derived.INT2, Derived.INT3, Derived.INT4);
		assertEquals(expected, changed);
		assertEquals(expected, edited);
	}

	@Test
	public void revert() {
		EntityConnection connection = employeeEditModel.connection();
		Entity martin = connection.selectSingle(Employee.NAME.equalTo("MARTIN"));
		editor.set(martin);

		editor.value(Employee.NAME).set("newname");
		assertTrue(editor.modified().is());
		editor.value(Employee.NAME).revert();
		assertFalse(editor.modified().is());

		editor.value(Employee.NAME).set("another");
		editor.value(Employee.HIREDATE).set(LocalDate.now());
		assertTrue(editor.modified().is());
		editor.revert();
		assertFalse(editor.modified().is());
	}

	@Test
	public void modified() {
		State extraModified = State.builder()
						.listener(editor.modified()::update)
						.build();
		editor.modified().predicate().map(modified -> modified.or(entity -> extraModified.is()));

		editor.set(employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("MARTIN")));
		extraModified.set(true);
		// UpdateException from the EntityConnection since the entity isn't really modified
		assertThrows(UpdateException.class, () -> employeeEditModel.update());
	}

	@Test
	public void validStates() {
		Entity martin = employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("MARTIN"));
		martin.remove(Employee.ID);// so it becomes new, in order for all attributes to be validated
		editor.set(martin);
		assertTrue(editor.valid().is());
		assertTrue(editor.value(Employee.NAME).valid().is());
		assertTrue(editor.value(Employee.SALARY).valid().is());
		editor.validator().set(new EntityValidator() {
			@Override
			public void validate(Entity entity, Attribute<?> attribute) throws ValidationException {
				if (attribute.equals(Employee.NAME) || attribute.equals(Employee.SALARY)) {
					throw new ValidationException(attribute, entity.get(attribute), "invalid");
				}
			}
		});
		assertFalse(editor.valid().is());
		assertFalse(editor.value(Employee.NAME).valid().is());
		assertFalse(editor.value(Employee.SALARY).valid().is());
	}

	@Test
	public void validateWithNonGeneratedPK() {
		TestEntityEditModel editModel = new TestEntityEditModel(NonGeneratedPK.TYPE, CONNECTION_PROVIDER);
		editModel.editor().value(NonGeneratedPK.ID).set(UUID.randomUUID());
		editModel.editor().value(NonGeneratedPK.NAME).set("123456");//length > 5
		assertThrows(ValidationException.class, editModel::insert);//works, due to the edit model setting the defaults

		Entity manual = editModel.entities().entity(NonGeneratedPK.TYPE).build();
		manual.set(NonGeneratedPK.ID, UUID.randomUUID());// non generated pk column initialized to null in entity builder, exists = false
		manual.set(NonGeneratedPK.NAME, "123456");
		assertThrows(ValidationException.class, () -> editModel.insert(singleton(manual)));
	}

	private static final class TestEntityEditModel extends AbstractEntityEditModel {

		private TestEntityEditModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
			super(entityType, connectionProvider);
		}
	}

	private static final class DetailEditModel extends AbstractEntityEditModel {

		private DetailEditModel(EntityConnectionProvider connectionProvider) {
			super(Detail.TYPE, connectionProvider);
		}
	}
}
