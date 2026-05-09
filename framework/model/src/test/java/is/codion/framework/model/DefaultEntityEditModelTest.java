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

import is.codion.common.model.CancelException;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Count;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.exception.UpdateEntityException;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityValidator;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ValueAttributeDefinition;
import is.codion.framework.domain.entity.exception.AttributeValidationException;
import is.codion.framework.domain.entity.exception.EntityValidationException;
import is.codion.framework.model.DefaultEntityModelTest.TestEntityEditModel;
import is.codion.framework.model.DefaultEntityModelTest.TestEntityEditor;
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

import static is.codion.framework.db.EntityConnection.Select.where;
import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

public final class DefaultEntityEditModelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final Entities ENTITIES = new TestDomain().entities();

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();
	private static final PersistenceEvents EMPLOYEE_PERSISTENCE_EVENTS = PersistenceEvents.persistenceEvents(Employee.TYPE);

	private DefaultEntityEditModel<?, ?, ?, ?> employeeEditModel;
	private EntityEditor<?> editor;

	@BeforeEach
	void setUp() {
		employeeEditModel = new TestEntityEditModel(Employee.TYPE, CONNECTION_PROVIDER);
		editor = employeeEditModel.editor();
		editor.value(Employee.HIREDATE).defaultValue().set(LocalDate::now);
		Entity jones = CONNECTION_PROVIDER.connection().selectSingle(Employee.ID.equalTo(3));//JONES, used in containsUnsavedData()
		editor.value(Employee.MGR_FK).defaultValue().set(() -> jones);
	}

	@Test
	void persistenceEvents() throws EntityValidationException {
		AtomicInteger insertEvents = new AtomicInteger();
		AtomicInteger updateEvents = new AtomicInteger();
		AtomicInteger deleteEvents = new AtomicInteger();
		AtomicBoolean managerInserted = new AtomicBoolean();

		Consumer<Collection<Entity>> insertListener = inserted -> insertEvents.incrementAndGet();
		Consumer<Map<Entity, Entity>> updateListener = updated -> updateEvents.incrementAndGet();
		Consumer<Collection<Entity>> deleteListener = deleted -> deleteEvents.incrementAndGet();

		EMPLOYEE_PERSISTENCE_EVENTS.inserted().addWeakConsumer(insertListener);
		EMPLOYEE_PERSISTENCE_EVENTS.updated().addWeakConsumer(updateListener);
		EMPLOYEE_PERSISTENCE_EVENTS.deleted().addWeakConsumer(deleteListener);
		EMPLOYEE_PERSISTENCE_EVENTS.inserted()
						.when(inserted -> inserted.stream()
										.anyMatch(entity -> "MANAGER".equals(entity.get(Employee.JOB))))
						.addListener(() -> managerInserted.set(true));

		employeeEditModel.editor().settings().publishPersistenceEvents().set(true);

		EntityConnection connection = employeeEditModel.connection();
		Entity jones = connection.selectSingle(Employee.NAME.equalTo("JONES"));
		connection.startTransaction();
		try {
			editor.entity().set(jones);
			editor.value(Employee.NAME).set("Noname");
			employeeEditModel.editor().insert();
			assertEquals(1, insertEvents.get());
			editor.value(Employee.NAME).set("Another");
			employeeEditModel.editor().update();
			assertEquals(1, updateEvents.get());
			assertNotNull(employeeEditModel.editor().delete());
			assertEquals(1, deleteEvents.get());
			assertTrue(managerInserted.get());
		}
		finally {
			connection.rollbackTransaction();
		}

		assertThrows(IllegalArgumentException.class, () -> PersistenceEvents.persistenceEvents(Department.TYPE)
						.inserted()
						.accept(singleton(jones)));
		assertThrows(IllegalArgumentException.class, () -> PersistenceEvents.persistenceEvents(Department.TYPE)
						.updated()
						.accept(singletonMap(jones, jones)));
		assertThrows(IllegalArgumentException.class, () -> PersistenceEvents.persistenceEvents(Department.TYPE)
						.deleted()
						.accept(singleton(jones)));

		EMPLOYEE_PERSISTENCE_EVENTS.inserted().removeWeakConsumer(insertListener);
		EMPLOYEE_PERSISTENCE_EVENTS.updated().removeWeakConsumer(updateListener);
		EMPLOYEE_PERSISTENCE_EVENTS.deleted().removeWeakConsumer(deleteListener);
	}

	@Test
	void searchModel() {
		EntitySearchModel model = employeeEditModel.editor().searchModels().get(Employee.DEPARTMENT_FK);
		assertNotNull(model);
		assertSame(model, employeeEditModel.editor().searchModels().get(Employee.DEPARTMENT_FK));
	}

	@Test
	void createSearchModel() {
		EntitySearchModel model = employeeEditModel.editor().searchModels().create(Employee.DEPARTMENT_FK);
		assertNotNull(model);
		assertEquals(Department.TYPE, model.entityDefinition().type());
	}

	@Test
	void refresh() {
		EntityConnection connection = employeeEditModel.connection();
		connection.startTransaction();
		try {
			Entity employee = connection.selectSingle(Employee.NAME.equalTo("MARTIN"));
			editor.entity().refresh();
			editor.entity().set(employee);
			employee.set(Employee.NAME, "NOONE");
			connection.update(employee);
			editor.entity().refresh();
			assertEquals("NOONE", editor.value(Employee.NAME).get());

			TestEntityEditModel departmentEditModel = new TestEntityEditModel(Department.TYPE, employeeEditModel.connectionProvider());
			Entity accounting = connection.selectSingle(Department.NAME.equalTo("ACCOUNTING"));
			TestEntityEditor departmentEditor = departmentEditModel.editor();
			departmentEditor.entity().set(accounting);
			departmentEditor.value(Department.ID).set(-20);

			accounting.set(Department.NAME, "Accounting");
			connection.update(accounting);
			departmentEditModel.editor().entity().refresh();
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
		editor.entity().set(employee);
		Entity copyWithPrimaryKeyValue = editor.entity().get();
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
		editor.entity().set(employee);
		//clear the department foreign key value
		Entity dept = editor.entity().get().entity(Employee.DEPARTMENT_FK);
		editor.value(Employee.DEPARTMENT_FK).clear();
		//set the reference key attribute value
		assertFalse(editor.value(Employee.DEPARTMENT_FK).present().is());
		editor.value(Employee.DEPARTMENT).set(dept.get(Department.ID));
		assertNull(editor.entity().get().get(Employee.DEPARTMENT_FK));
		dept = editor.value(Employee.DEPARTMENT_FK).get();
		assertNull(dept);
		editor.values().defaults();
		assertNotNull(editor.value(Employee.DEPARTMENT_FK).get());
	}

	@Test
	void defaults() {
		editor.value(Employee.NAME).defaultValue().set(() -> "Scott");
		assertFalse(editor.value(Employee.NAME).present().is());
		editor.values().defaults();
		assertEquals("Scott", editor.value(Employee.NAME).get());

		editor.value(Employee.NAME).defaultValue().set(() -> null);
		editor.values().defaults();
		assertFalse(editor.value(Employee.NAME).present().is());
	}

	@Test
	void clear() {
		Entity employee = employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("MARTIN"));
		editor.entity().set(employee);
		editor.values().defaults();
		assertTrue(editor.value(Employee.DEPARTMENT_FK).present().is());//persists
		assertFalse(editor.value(Employee.NAME).present().is());
		editor.entity().set(employee);
		editor.values().clear();
		assertFalse(editor.value(Employee.DEPARTMENT_FK).present().is());//should not persist on clear
		assertFalse(editor.value(Employee.NAME).present().is());
	}

	@Test
	void test() {
		ObservableState primaryKeyPresentState = editor.primaryKeyPresent();
		ObservableState entityExistsState = editor.exists();

		assertFalse(primaryKeyPresentState.is());
		assertFalse(entityExistsState.is());

		Consumer<Object> consumer = data -> {};
		employeeEditModel.editor().events().after().delete().addConsumer(consumer);
		employeeEditModel.editor().events().after().insert().addConsumer(consumer);
		employeeEditModel.editor().events().after().update().addConsumer(consumer);
		employeeEditModel.editor().events().before().delete().addConsumer(consumer);
		employeeEditModel.editor().events().before().insert().addConsumer(consumer);
		employeeEditModel.editor().events().before().update().addConsumer(consumer);
		Runnable listener = () -> {};
		employeeEditModel.editor().events().persisted().addListener(listener);

		assertEquals(Employee.TYPE, employeeEditModel.entityType());

		assertFalse(editor.exists().is());
		assertFalse(editor.modified().is());

		Entity employee = employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("MARTIN"));
		editor.entity().set(employee);
		assertTrue(primaryKeyPresentState.is());
		assertTrue(entityExistsState.is());

		assertTrue(editor.entity().get().equalValues(employee), "Active entity is not equal to the entity just set");
		assertTrue(editor.exists().is(), "Active entity exists after an entity is set");
		assertFalse(editor.modified().is());
		editor.values().defaults();
		assertFalse(editor.exists().is(), "Active entity exists after defaults are set");
		assertFalse(editor.modified().is());
		assertTrue(editor.entity().get().primaryKey().isNull(), "Active entity primary key is not null after defaults are set");

		editor.entity().set(employee);
		assertFalse(editor.entity().get().primaryKey().isNull(), "Active entity primary key is null after entity is set");

		Integer originalEmployeeId = editor.value(Employee.ID).get();
		editor.value(Employee.ID).clear();
		assertFalse(primaryKeyPresentState.is());
		editor.value(Employee.ID).set(originalEmployeeId);
		assertTrue(primaryKeyPresentState.is());

		editor.values().defaults();
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
		catch (AttributeValidationException e) {
			assertEquals(Employee.COMMISSION, e.attribute());
			assertEquals(50d, e.value());
			ValueAttributeDefinition<?> attributeDefinition = (ValueAttributeDefinition<?>)
							ENTITIES.definition(Employee.TYPE).attributes().definition(e.attribute());
			assertTrue(e.getMessage().contains(attributeDefinition.toString()));
			assertTrue(e.getMessage().contains(attributeDefinition.minimum().map(Objects::toString).get()));
		}

		editor.values().defaults();
		assertTrue(editor.entity().get().primaryKey().isNull(), "Active entity is not null after model is cleared");

		employeeEditModel.editor().events().after().delete().removeConsumer(consumer);
		employeeEditModel.editor().events().after().insert().removeConsumer(consumer);
		employeeEditModel.editor().events().after().update().removeConsumer(consumer);
		employeeEditModel.editor().events().before().delete().removeConsumer(consumer);
		employeeEditModel.editor().events().before().insert().removeConsumer(consumer);
		employeeEditModel.editor().events().before().update().removeConsumer(consumer);
		employeeEditModel.editor().events().persisted().removeListener(listener);
	}

	@Test
	void insertReadOnly() throws CancelException {
		employeeEditModel.editor().settings().readOnly().set(true);
		assertThrows(IllegalStateException.class, () -> employeeEditModel.editor().insert());
	}

	@Test
	void updateReadOnly() throws CancelException {
		employeeEditModel.editor().settings().readOnly().set(true);
		assertThrows(IllegalStateException.class, () -> employeeEditModel.editor().update());
	}

	@Test
	void deleteReadOnly() throws CancelException {
		employeeEditModel.editor().settings().readOnly().set(true);
		assertThrows(IllegalStateException.class, () -> employeeEditModel.editor().delete());
	}

	@Test
	void insert() throws EntityValidationException {
		assertTrue(employeeEditModel.editor().insert(emptyList()).isEmpty());
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

			employeeEditModel.editor().events().after().insert().addConsumer(insertedEntities ->
							assertEquals(department, insertedEntities.iterator().next().get(Employee.DEPARTMENT_FK)));
			employeeEditModel.editor().settings().insertEnabled().set(false);
			assertFalse(employeeEditModel.editor().settings().insertEnabled().is());
			assertThrows(IllegalStateException.class, () -> employeeEditModel.editor().insert());
			employeeEditModel.editor().settings().insertEnabled().set(true);
			assertTrue(employeeEditModel.editor().settings().insertEnabled().is());

			employeeEditModel.editor().insert();
			assertTrue(editor.exists().is());
			Entity entityCopy = editor.entity().get();
			assertFalse(entityCopy.primaryKey().isNull());
			assertEquals(entityCopy.primaryKey(), entityCopy.originalPrimaryKey());

			editor.value(Employee.NAME).set("Bobby");
			try {
				employeeEditModel.editor().insert();
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
	void update() throws EntityValidationException {
		assertTrue(employeeEditModel.editor().update(emptyList()).isEmpty());
		EntityConnection connection = employeeEditModel.connection();
		connection.startTransaction();
		try {
			AtomicInteger replacedCount = new AtomicInteger();
			editor.entity().replaced().addListener(replacedCount::incrementAndGet);
			editor.entity().set(connection.selectSingle(Employee.NAME.equalTo("MILLER")));
			assertFalse(editor.modified().is());
			editor.value(Employee.NAME).set("BJORN");
			assertTrue(editor.modified().is());
			List<Entity> toUpdate = singletonList(editor.entity().get());
			Consumer<Map<Entity, Entity>> consumer = updatedEntities ->
							assertEquals(toUpdate, new ArrayList<>(updatedEntities.values()));
			employeeEditModel.editor().events().after().update().addConsumer(consumer);
			employeeEditModel.editor().settings().updateEnabled().set(false);
			assertFalse(employeeEditModel.editor().settings().updateEnabled().is());
			assertThrows(IllegalStateException.class, () -> employeeEditModel.editor().update());
			employeeEditModel.editor().settings().updateEnabled().set(true);
			assertTrue(employeeEditModel.editor().settings().updateEnabled().is());

			employeeEditModel.editor().update();
			assertFalse(editor.modified().is());
			assertEquals(1, replacedCount.get());
			employeeEditModel.editor().events().after().update().removeConsumer(consumer);

			employeeEditModel.editor().settings().updateMultipleEnabled().set(false);

			Entity emp1 = connection.selectSingle(Employee.NAME.equalTo("BLAKE"));
			emp1.set(Employee.COMMISSION, 100d);
			Entity emp2 = connection.selectSingle(Employee.NAME.equalTo("JONES"));
			emp2.set(Employee.COMMISSION, 100d);
			assertThrows(IllegalStateException.class, () -> employeeEditModel.editor().update(Arrays.asList(emp1, emp2)));

			// Test afterUpdate event map contents
			TestEntityEditModel deptEditModel = new TestEntityEditModel(Department.TYPE, CONNECTION_PROVIDER);
			deptEditModel.editor().value(Department.ID).set(-1);
			deptEditModel.editor().value(Department.NAME).set("UpdTest");
			Entity dept = deptEditModel.editor().insert();
			deptEditModel.editor().entity().set(dept);

			AtomicBoolean checker = new AtomicBoolean(false);

			deptEditModel.editor().events().after().update().addConsumer(updated -> {
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
			deptEditModel.editor().update();

			assertTrue(checker.get(), "After update event should have been triggered");
		}
		finally {
			connection.rollbackTransaction();
			employeeEditModel.editor().settings().updateMultipleEnabled().set(true);
		}
	}

	@Test
	void delete() {
		assertTrue(employeeEditModel.editor().delete(emptyList()).isEmpty());
		EntityConnection connection = employeeEditModel.connection();
		connection.startTransaction();
		try {
			editor.entity().set(connection.selectSingle(Employee.NAME.equalTo("MILLER")));
			List<Entity> toDelete = singletonList(editor.entity().get());
			employeeEditModel.editor().events().after().delete().addConsumer(deletedEntities -> assertTrue(toDelete.containsAll(deletedEntities)));
			employeeEditModel.editor().settings().deleteEnabled().set(false);
			assertFalse(employeeEditModel.editor().settings().deleteEnabled().is());
			assertThrows(IllegalStateException.class, () -> employeeEditModel.editor().delete());
			employeeEditModel.editor().settings().deleteEnabled().set(true);
			assertTrue(employeeEditModel.editor().settings().deleteEnabled().is());
			editor.value(Employee.ID).set(3);// modify primary key to JONES, should be reverted before delete
			assertEquals(1, connection.count(Count.where(Employee.NAME.equalTo("JONES"))));// JONES was not deleted

			employeeEditModel.editor().delete();
		}
		finally {
			connection.rollbackTransaction();
		}
	}

	@Test
	void setEntity() {
		Entity martin = employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("MARTIN"));
		Entity king = employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("KING"));
		editor.entity().set(king);
		editor.value(Employee.MGR_FK).set(martin);
		editor.values().defaults();
		king.set(Employee.MGR_FK, null);
		editor.entity().set(king);
		assertNull(editor.value(Employee.MGR_FK).get());
		editor.values().defaults();
		assertEquals(LocalDate.now(), editor.value(Employee.HIREDATE).get());
		assertFalse(editor.entity().get().modified(Employee.HIREDATE));
		assertFalse(editor.entity().get().modified());
	}

	@Test
	void modifiedAttributes() {
		Entity martin = employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("MARTIN"));
		Entity king = employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("KING"));
		Entity martinsManager = martin.get(Employee.MGR_FK);
		editor.entity().set(martin);

		assertEquals(martinsManager, editor.value(Employee.MGR_FK).get());
		editor.value(Employee.MGR_FK).clear();
		assertEquals(martinsManager, editor.value(Employee.MGR_FK).original());
		assertTrue(editor.modified().attributes().containsOnly(singleton(Employee.MGR_FK)));
		editor.value(Employee.MGR_FK).revert();
		assertEquals(martinsManager, editor.value(Employee.MGR_FK).get());
		assertTrue(editor.modified().attributes().isEmpty());

		editor.value(Employee.NAME).set("NewName");
		assertEquals("MARTIN", editor.value(Employee.NAME).original());
		assertTrue(editor.modified().attributes().containsOnly(singleton(Employee.NAME)));
		editor.value(Employee.MGR_FK).set(king);
		assertEquals(martinsManager, editor.value(Employee.MGR_FK).original());
		assertTrue(editor.modified().attributes().containsOnly(Arrays.asList(Employee.MGR_FK, Employee.NAME)));
		editor.value(Employee.SALARY).set(3210d);
		assertTrue(editor.modified().attributes().containsOnly(Arrays.asList(Employee.SALARY, Employee.NAME, Employee.MGR_FK)));
	}

	@Test
	void persist() {
		Entity king = employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("KING"));
		editor.entity().set(king);
		assertNotNull(editor.value(Employee.JOB).get());
		editor.value(Employee.JOB).persist().set(true);
		editor.values().defaults();
		assertNotNull(editor.value(Employee.JOB).get());
		editor.entity().set(king);
		editor.value(Employee.JOB).persist().set(false);
		editor.values().defaults();
		assertNull(editor.value(Employee.JOB).get());
		assertThrows(IllegalArgumentException.class, () -> editor.value(Department.ID).persist().set(true));
		assertThrows(IllegalArgumentException.class, () -> editor.value(Department.ID).persist().is());
	}

	@Test
	void updated() throws EntityValidationException {
		EntityConnection connection = employeeEditModel.connection();
		connection.startTransaction();
		try {
			Entity james = connection
							.selectSingle(Employee.NAME.equalTo("JAMES"));
			editor.entity().set(james);
			assertTrue(editor.entity().get().entity(Employee.MGR_FK).isNull(Employee.COMMISSION));
			TestEntityEditModel blakeEditModel = new TestEntityEditModel(Employee.TYPE, CONNECTION_PROVIDER);
			blakeEditModel.editor().entity().set(connection
							.selectSingle(Employee.NAME.equalTo("BLAKE")));
			blakeEditModel.editor().value(Employee.COMMISSION).set(100d);
			blakeEditModel.editor().update();
			assertEquals(100d, editor.entity().get().entity(Employee.MGR_FK).get(Employee.COMMISSION));
		}
		finally {
			connection.rollbackTransaction();
		}
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
		TestEntityEditModel editModel = new TestEntityEditModel(Detail.TYPE, employeeEditModel.connectionProvider());

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
		editModel.editor().entity().set(detail);
		assertEquals(3, derivedCounter.get());
		assertEquals(2, derivedEditCounter.get());
	}

	@Test
	void persistWritableForeignKey() {
		TestEntityEditModel editModel = new TestEntityEditModel(Detail.TYPE, employeeEditModel.connectionProvider());
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
		assertTrue(editor.value(Employee.NAME).modified().is());
		Entity martin = employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("MARTIN"));
		editor.entity().set(martin);
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
		editor.entity().set(martin);
		assertFalse(nameModifiedObserver.is());
		assertFalse(modifiedState.is());
		assertFalse(nameIsNull.is());
		assertFalse(nullState.is());
	}

	@Test
	void modifiedUpdate() throws EntityValidationException {
		EntityConnection connection = employeeEditModel.connection();
		ObservableState nameModifiedObserver = editor.value(Employee.NAME).modified();
		Entity martin = connection.selectSingle(Employee.NAME.equalTo("MARTIN"));
		editor.entity().set(martin);
		editor.value(Employee.NAME).set("MARTINEZ");
		assertTrue(nameModifiedObserver.is());
		connection.startTransaction();
		try {
			employeeEditModel.editor().update();
			assertFalse(nameModifiedObserver.is());
		}
		finally {
			connection.rollbackTransaction();
		}
	}

	@Test
	public void derivedValues() {
		TestEntityEditModel editModel = new TestEntityEditModel(Derived.TYPE, CONNECTION_PROVIDER);

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
		editor.entity().set(martin);

		editor.value(Employee.NAME).set("newname");
		assertTrue(editor.modified().is());
		editor.value(Employee.NAME).revert();
		assertFalse(editor.modified().is());

		editor.value(Employee.NAME).set("another");
		editor.value(Employee.HIREDATE).set(LocalDate.now());
		assertTrue(editor.modified().is());
		editor.values().revert();
		assertFalse(editor.modified().is());
	}

	@Test
	public void modified() {
		State extraModified = State.state();
		editor.modified().additional().add(extraModified);

		editor.entity().set(employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("MARTIN")));
		assertFalse(editor.modified().is());
		extraModified.set(true);
		assertTrue(editor.modified().is());
		extraModified.set(false);
		assertFalse(editor.modified().is());
		editor.modified().additional().remove(extraModified);
		State extraModified2 = State.state(false);
		editor.modified().additional().add(extraModified2);
		assertFalse(editor.modified().is());
		extraModified.set(true);// not listened to anymore
		assertFalse(editor.modified().is());
		extraModified2.set(true);
		assertTrue(editor.modified().is());
		// UpdateException from the EntityConnection since the entity isn't really modified
		assertThrows(UpdateEntityException.class, () -> employeeEditModel.editor().update());
	}

	@Test
	public void validStates() {
		Entity martin = employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("MARTIN"));
		martin.remove(Employee.ID);// so it becomes new, in order for all attributes to be validated
		editor.entity().set(martin);
		assertTrue(editor.valid().is());
		assertTrue(editor.value(Employee.NAME).valid().is());
		assertTrue(editor.value(Employee.SALARY).valid().is());
		editor.validator().set(new EntityValidator() {
			@Override
			public void validate(Entity entity, Attribute<?> attribute) throws AttributeValidationException {
				if (attribute.equals(Employee.NAME) || attribute.equals(Employee.SALARY)) {
					throw new AttributeValidationException(attribute, entity.get(attribute), "invalid");
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
		assertThrows(EntityValidationException.class, editModel.editor()::insert);//works, due to the edit model setting the defaults

		Entity manual = editModel.entities().entity(NonGeneratedPK.TYPE).build();
		manual.set(NonGeneratedPK.ID, UUID.randomUUID());// non generated pk column initialized to null in entity builder, exists = false
		manual.set(NonGeneratedPK.NAME, "123456");
		assertThrows(EntityValidationException.class, () -> editModel.editor().insert(singleton(manual)));
	}

	@Test
	void refreshLazy() {
		EntityConnection connection = employeeEditModel.connection();
		Entity jones = connection.selectSingle(where(Employee.NAME.equalTo("JONES")).build());

		TestEntityEditModel editModel = new TestEntityEditModel(Employee.TYPE, CONNECTION_PROVIDER);
		assertFalse(editModel.editor().entity().get().contains(Employee.DATA));
		editModel.editor().entity().set(jones);
		assertFalse(editModel.editor().entity().get().contains(Employee.DATA));
		editModel.editor().entity().refresh();
		assertFalse(editModel.editor().entity().get().contains(Employee.DATA));

		jones = connection.selectSingle(where(Employee.NAME.equalTo("JONES"))
						.include(Employee.DATA)
						.build());
		editModel.editor().entity().set(jones);
		assertTrue(editModel.editor().entity().get().contains(Employee.DATA));
		editModel.editor().entity().refresh();
		assertTrue(editModel.editor().entity().get().contains(Employee.DATA));
	}
}
